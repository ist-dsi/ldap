package pt.tecnico.dsi.ldap


import com.typesafe.scalalogging.LazyLogging
import org.ldaptive.pool._
import org.ldaptive._

import scala.collection.JavaConverters._
import scala.concurrent.Future

class Ldap(val settings: Settings = new Settings()) extends LazyLogging {

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

  private def withConnection[R](f: Connection => R): Future[R] = Future {
    connectionFactory.getConnection
  }.flatMap { connection =>
    try {
      if (!connection.isOpen) {
        connection.open()
      }

      Future(f(connection))
    } catch {
      case ex: LdapException | IllegalStateException => Future.failed(ex)
    } finally {
      connection.close()
    }
  }

  def addAttributes(dn: String, attributes: Map[String, String]): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.ADD, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)
  }

  def replaceAttributes(dn: String, attributes: Map[String, String]): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { case (name, value) =>
      new AttributeModification(AttributeModificationType.REPLACE, new LdapAttribute(name, value))
    }.toSeq

    executeModifyOperation(dn, attributesModification)
  }

  def removeAttributes(dn: String, attributes: Seq[String]): Future[Unit] = {
    val attributesModification: Seq[AttributeModification] = attributes.map { attribute =>
      new AttributeModification(AttributeModificationType.REMOVE, new LdapAttribute(attribute))
    }

    executeModifyOperation(dn, attributesModification)
  }

  private def executeModifyOperation(dn: String, attributes: Seq[AttributeModification]): Future[Unit] = withConnection {
    connection => new ModifyOperation(connection).execute(new ModifyRequest(dn, attributes: _*))
  }

  private def createSearchResult(ou: String, filter: String, attributes: Seq[String])(implicit connection: Connection) = {
    val request = new SearchRequest(s"$ou,$baseDomain", filter, attributes: _*)
    request.setDerefAliases(DerefAliases.valueOf(searchDereferenceAlias))
    request.setSearchScope(SearchScope.valueOf(searchScope))
    request.setSizeLimit(searchSizeLimit)
    request.setTimeLimit(searchTimeLimit)

    new SearchOperation(connection).execute(request).getResult
  }

  def search(ou: String, filter: String, attributes: Seq[String] = Seq.empty): Future[Option[Entry]] =
    withConnection { connection =>
      val result: LdapEntry = createSearchResult(ou, filter, attributes).getEntry
      fixLdapEntry(result)
    }

  def searchAll(ou: String, filter: String, attributes: Seq[String] = Seq.empty): Future[Seq[Entry]] =
    withConnection { connection =>
      val results: Iterable[LdapEntry] = createSearchResult(ou, filter, attributes).getEntries.asScala
      results.toSeq.flatMap(fixLdapEntry)
    }


  private def fixLdapEntry(entry: LdapEntry): Option[Entry] = {
    Option(entry)
      .filter(_.getAttributes != null)
      .map { e =>
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

}
