package pt.tecnico.dsi.ldap.operations

case class Search(dc: String, filter: String, attributes: Seq[String] = Seq.empty)
