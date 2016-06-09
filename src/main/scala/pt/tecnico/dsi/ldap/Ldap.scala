package pt.tecnico.dsi.ldap

import com.typesafe.scalalogging.LazyLogging
import org.ldaptive.pool._
import org.ldaptive._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Ldap(private val settings: Settings = new Settings()) extends LazyLogging {

  private val connectionFactory: ConnectionFactory = if (settings.enablePool) {
    settings.pool.initialize()
    logger.info("Connection pool initialized successfully")
    settings.pooledConnectionFactory
  } else {
    settings.defaultConnectionFactory
  }

  def close(): Unit = connectionFactory match {
    case cf: PooledConnectionFactory =>
      logger.info("Closing connection pool")
      cf.getConnectionPool.close()
    case _ => //Nothing to do
  }

  private def logAvailableConnectionsInPool: String = connectionFactory match {
    case cf: PooledConnectionFactory => s"${cf.getConnectionPool.availableCount()} connections available"
    case _ => ""//Nothing to do
  }

  private def withConnection[R](f: Connection => R)(implicit ex: ExecutionContext): Future[R] = Future {
    logger.debug(s"${logAvailableConnectionsInPool}Obtaining connection")
    connectionFactory.getConnection
  } flatMap { connection =>
    Try {
      if (!connection.isOpen) {
        logger.debug("Connection opened")
        connection.open()
      }

      val result = Future(f(connection))

      result.onComplete { _ =>
        connection.close()
        logger.debug(s"Connection closed $logAvailableConnectionsInPool")
      }

      result
    } match {
      case Success(result) => result
      case Failure(exception) => Future.failed(exception)
    }
  }

  def addEntry(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = withConnection { connection =>
    logger.info(s"Adding ${appendBaseDn(dn)} with ${attributesToString(attributes)}")
    val ldapAttributes: Seq[LdapAttribute] = attributes.map { case (name, value) =>
      new LdapAttribute(name, value)
    }.toSeq

    val operation = new AddOperation(connection)
    addOperationHandler(operation)
    operation.execute(new AddRequest(appendBaseDn(dn), ldapAttributes.asJavaCollection))
  }

  def deleteEntry(dn: String)(implicit ex: ExecutionContext): Future[Unit] = withConnection { connection =>
    logger.info(s"Deleting entry ${appendBaseDn(dn)}")
    val operation = new DeleteOperation(connection)
    addOperationHandler(operation)
    operation.execute(new DeleteRequest(appendBaseDn(dn)))
  }


  def addAttributes(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.ADD, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)(s"Adding a ${attributesToString(attributes)} attributes for ${appendBaseDn(dn)}")
  }

  def replaceAttributes(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.REPLACE, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)(s"Replacing ${attributesToString(attributes)} attributes for ${appendBaseDn(dn)}")
  }

  def removeAttributes(dn: String, attributes: Seq[String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { attribute =>
      new AttributeModification(AttributeModificationType.REMOVE, new LdapAttribute(attribute))
    }

    executeModifyOperation(dn, attributesModification)(s"Removing ${attributes.mkString(", ")} attributes for ${appendBaseDn(dn)}")
  }

  private def executeModifyOperation(dn: String, attributes: Seq[AttributeModification])
                                    (logMessage: String)
                                    (implicit ex: ExecutionContext): Future[Unit] = {
    withConnection { connection =>
      logger.info(logMessage)
      val operation = new ModifyOperation(connection)
      addOperationHandler(operation)
      operation.execute(new ModifyRequest(appendBaseDn(dn), attributes: _*))
    }
  }

  private def createSearchResult(dn: String, filter: String, attributes: Seq[String], size: Int)
                                (implicit connection: Connection) = {
    val request = new SearchRequest(appendBaseDn(dn), filter, attributes: _*)
    request.setDerefAliases(DerefAliases.valueOf(settings.searchDereferenceAlias))
    request.setSearchScope(SearchScope.valueOf(settings.searchScope))
    request.setSizeLimit(size)
    request.setTimeLimit(settings.searchTimeLimit.toMillis)

    val operation: SearchOperation = new SearchOperation(connection)
    addOperationHandler(operation)
    operation.execute(request).getResult
  }

  def search(dn: String, filter: String, returningAttributes: Seq[String] = Seq.empty, size: Int = settings.searchSizeLimit)
            (implicit ex: ExecutionContext): Future[Seq[Entry]] = {
    withConnection { implicit connection =>
      logger.info(s"Performing a search($size) for ${appendBaseDn(dn)}, with $filter and returning ${returningAttributes.mkString(", ")} attributes")
      val result: SearchResult = createSearchResult(dn, filter, returningAttributes, size)

      if (size == 1) {
        fixLdapEntry(result.getEntry).toSeq
      } else {
        result.getEntries.asScala.toSeq.flatMap(fixLdapEntry)
      }
    }
  }

  def searchAll(dn: String, filter: String, returningAttributes: Seq[String] = Seq.empty)
               (implicit ex: ExecutionContext): Future[Seq[Entry]] = {
    search(dn, filter, returningAttributes, 0)
  }

  private def addOperationHandler[R <: Request, S](operation: AbstractOperation[R,S]): Unit = {
    val handler = new operation.ReopenOperationExceptionHandler()

    operation.setOperationExceptionHandler(handler)
  }

  private def fixLdapEntry(entry: LdapEntry): Option[Entry] = {
    Option(entry).filter {
      e =>
        Some(e.getAttributes).isDefined
    }.map(fixLdapAttribute)
  }

  private def fixLdapAttribute(e: LdapEntry): Entry = {
    val (binaryAttributes, textAttributes) = e.getAttributes.asScala.partition(_.isBinary)
    val dn = Option(e.getDn)
    val mappedTextAttributes = textAttributes.map(toLdapStringAttribute).toMap
    val mappedBinaryAttributes = binaryAttributes.map(toLdapBinaryAttribute).toMap

    Entry(dn, mappedTextAttributes, mappedBinaryAttributes)
  }

  private def toLdapStringAttribute(ldapAttribute: LdapAttribute): (String, Seq[String]) = {
    require(!ldapAttribute.isBinary)
    ldapAttribute.getName -> ldapAttribute.getStringValues.asScala.toSeq
  }

  private def toLdapBinaryAttribute(ldapAttribute: LdapAttribute): (String, Seq[Array[Byte]]) = {
    require(ldapAttribute.isBinary)
    ldapAttribute.getName -> ldapAttribute.getBinaryValues.asScala.toSeq
  }

  private def appendBaseDn(dn: String): String = s"$dn,${settings.baseDomain}"

  private def attributesToString(attributes: Map[String, String]): String = attributes.mkString("[ ", " ; ", " ]")

}
