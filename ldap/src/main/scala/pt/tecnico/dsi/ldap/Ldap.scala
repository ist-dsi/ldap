package pt.tecnico.dsi.ldap

import com.typesafe.scalalogging.LazyLogging
import org.ldaptive.pool._
import org.ldaptive._

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

  def search(ou: String, filter: String, attributes: String*): Future[SearchResult] = withConnection { connection =>
    val request = new SearchRequest(s"$ou,$baseDomain", filter, attributes: _*)
    request.setDerefAliases(DerefAliases.valueOf(searchDereferenceAlias))
    request.setSearchScope(SearchScope.valueOf(searchScope))
    request.setSizeLimit(searchSizeLimit)
    request.setTimeLimit(searchTimeLimit)

    new SearchOperation(connection).execute(request).getResult
  }

  private def fixLdapEntry(entry: LdapEntry): Option[Entry] = {
    import scala.collection.JavaConverters._

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
