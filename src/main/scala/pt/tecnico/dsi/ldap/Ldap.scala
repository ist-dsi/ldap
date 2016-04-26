package pt.tecnico.dsi.ldap

import com.typesafe.scalalogging.LazyLogging
import org.ldaptive.pool._
import org.ldaptive._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Ldap(private val settings: Settings = new Settings()) extends LazyLogging {

  import settings._

  private val connectionFactory: ConnectionFactory = if (enablePool) {
    pool.initialize()
    logger.info("Connection pool initialized successfully.")
    pooledConnectionFactory
  } else {
    defaultConnectionFactory
  }

  def close(): Unit = connectionFactory match {
    case cf: PooledConnectionFactory =>
      pool.close()
    case _ => //Nothing to do
  }

  private def withConnection[R](f: Connection => R)(implicit ex: ExecutionContext): Future[R] = Future {
    connectionFactory.getConnection
  } flatMap { connection =>
    Try {
      if (!connection.isOpen) {
        connection.open()
      }

      Future(f(connection))
    } match {
      case Success(result) => result
      case Failure(exception) => Future.failed(exception)
    }
  }

  def addEntry(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = withConnection { connection =>
    val ldapAttributes: Seq[LdapAttribute] = attributes.map { case (name, value) =>
      new LdapAttribute(name, value)
    }.toSeq

    new AddOperation(connection).execute(new AddRequest(appendBaseDn(dn), ldapAttributes.asJavaCollection))
  }

  def deleteEntry(dn: String)(implicit ex: ExecutionContext): Future[Unit] =
    withConnection(new DeleteOperation(_).execute(new DeleteRequest(dn)))

  def addAttributes(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.ADD, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)
  }

  def replaceAttributes(dn: String, attributes: Map[String, String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.REPLACE, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)
  }

  def removeAttributes(dn: String, attributes: Seq[String])(implicit ex: ExecutionContext): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { attribute =>
      new AttributeModification(AttributeModificationType.REMOVE, new LdapAttribute(attribute))
    }

    executeModifyOperation(dn, attributesModification)
  }

  private def executeModifyOperation(dn: String, attributes: Seq[AttributeModification])
                                    (implicit ex: ExecutionContext): Future[Unit] =
    withConnection { connection =>
      new ModifyOperation(connection).execute(new ModifyRequest(appendBaseDn(dn), attributes: _*))
    }

  private def createSearchResult(dn: String, filter: String, attributes: Seq[String])
                                (implicit connection: Connection) = {
    val request = new SearchRequest(appendBaseDn(dn), filter, attributes: _*)
    request.setDerefAliases(DerefAliases.valueOf(searchDereferenceAlias))
    request.setSearchScope(SearchScope.valueOf(searchScope))
    request.setSizeLimit(searchSizeLimit)
    request.setTimeLimit(searchTimeLimit)

    new SearchOperation(connection).execute(request).getResult
  }

  def search(dn: String, filter: String, attributes: Seq[String] = Seq.empty)
            (implicit ex: ExecutionContext): Future[Option[Entry]] =
    withConnection { implicit connection =>
      val result: LdapEntry = createSearchResult(dn, filter, attributes).getEntry
      fixLdapEntry(result)
    }

  def searchAll(dn: String, filter: String, attributes: Seq[String] = Seq.empty)
               (implicit ex: ExecutionContext): Future[Seq[Entry]] =
    withConnection { implicit connection =>
      val results: Iterable[LdapEntry] = createSearchResult(dn, filter, attributes).getEntries.asScala
      results.toSeq.flatMap(fixLdapEntry)
    }


  private def fixLdapEntry(entry: LdapEntry): Option[Entry] = {
    Option(entry)
      .filter { e =>
        Some(e.getAttributes).isDefined
      }.map { e =>
      val attributes = e.getAttributes.asScala.map { attribute =>
        val values = if (attribute.isBinary) {
          attribute.getBinaryValues.asScala.map { value =>
            Binary(value)
          }.toSeq
        } else {
          attribute.getStringValues.asScala.map { value =>
            Text(value)
          }.toSeq
        }
        (attribute.getName, values)
      }.toMap

      Entry(Option(e.getDn), attributes)
    }
  }

  private def appendBaseDn(dn: String): String = s"$dn,$baseDomain"

}
