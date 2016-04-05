package pt.tecnico.dsi.ldap

case class Entry(dn: Option[String], private val attributes: Map[String, Seq[Value]]) {
  def value(name: String): Option[Value#T] = attributes.get(name).flatMap(_.headOption).map(_.value)
  def values(name: String): Seq[Value#T] = attributes.getOrElse(name, Seq.empty).map(_.value)
}

sealed trait Value {
  type T //TODO put an awesome comment here bashing the lack of OOP in Ldaptive (this is the price to pay for using Ldaptive
  def value: T
}

case class Binary(value: Array[Byte]) extends Value {
  type T = Array[Byte]
}
case class Text(value: String) extends Value {
  type T = String
}
