package pt.tecnico.dsi.ldap

case class Entry(dn: Option[String], private val textAttributes: Map[String, List[String]],
                 private val binaryAttributes: Map[String, List[Array[Byte]]]) {
  def textValue(name: String): Option[String] = textAttributes.get(name).flatMap(_.headOption)
  def binaryValue(name: String): Option[Array[Byte]] = binaryAttributes.get(name).flatMap(_.headOption)

  def textValues(name: String): List[String] = textAttributes.getOrElse(name, List.empty)
  def binaryValues(name: String): List[Array[Byte]] = binaryAttributes.getOrElse(name, List.empty)

}