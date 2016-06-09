package pt.tecnico.dsi.ldap.pool

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import pt.tecnico.dsi.ldap.pool.Messages._

trait ConnectionPool extends Actor with ActorLogging {

  private def get() = {

  }

  private def put() = {

  }

  private def validateConnection() = {

  }

  private def growPool() = {

  }

  private def prunePool() = {

  }

  private def initiliazePool() = {

  }

  private def closePool() = {

  }

  def receive: Receive = LoggingReceive {
    case Grow => growPool()
    case GetConnection => get()
    case ReturnConnection => put()
    case Prune => prunePool()
    case ValidateConnection => validateConnection()
    case Close => closePool()
  }
}
