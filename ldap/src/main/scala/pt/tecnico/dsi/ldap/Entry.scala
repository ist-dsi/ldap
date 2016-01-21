package pt.tecnico.dsi.ldap

case class Entry[T, V <: Value[T]](dn: String, private val attributes: Map[String, Seq[V]]) {
  def value(name: String): Option[T] = attributes.get(name).flatMap(_.headOption).map(_.value)
  def values(name: String): Seq[T] = attributes.getOrElse(name, Seq.empty).map(_.value)

}

sealed trait Value[T] {
  def value: T
}

case class Text(value: String) extends Value[String]
case class ByteArray(value: Array[Byte]) extends Value[Array[Byte]]