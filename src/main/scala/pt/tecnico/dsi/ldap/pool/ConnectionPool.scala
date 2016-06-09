package pt.tecnico.dsi.ldap.pool

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import pt.tecnico.dsi.ldap.pool.Messages._

trait ConnectionPool extends Actor with ActorLogging {

  def receive: Receive = LoggingReceive {
    case Grow => ???
    case GetConnection => ???
    case ReturnConnection => ???
    case Prune => ???
    case ValidateConnection => ???
    case Close => ???
  }
}
