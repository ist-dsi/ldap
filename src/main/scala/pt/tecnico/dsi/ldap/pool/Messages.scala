package pt.tecnico.dsi.ldap.pool

import org.ldaptive.{Connection => LdaptiveConnection}

object Messages {

  sealed trait Request
  sealed trait Response

  case class Grow() extends Request
  case class GetConnection() extends Request
  case class ReturnConnection() extends Request
  case class Prune() extends Request
  case class ValidateConnection() extends Request
  case class Close() extends Request

  case class Connection(value: LdaptiveConnection) extends Response
}
