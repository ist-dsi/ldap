package pt.tecnico.dsi.ldap

case class Entry(dn: Option[String], private val textAttributes: Map[String, Seq[String]],
                 private val binaryAttributes: Map[String, Seq[Array[Byte]]]) {
  def textValue(name: String): Option[String] = textAttributes.get(name).flatMap(_.headOption)
  def binaryValue(name: String): Option[Array[Byte]] = binaryAttributes.get(name).flatMap(_.headOption)

  def textValues(name: String): Seq[String] = textAttributes.getOrElse(name, Seq.empty)
  def binaryValues(name: String): Seq[Array[Byte]] = binaryAttributes.getOrElse(name, Seq.empty)

}