package pt.tecnico.dsi.ldap

import org.scalatest.{AsyncFlatSpec, Matchers, OptionValues}

abstract class UnitSpec extends AsyncFlatSpec with Matchers with OptionValues {

  val settings = new Settings()
  val simpleLdap = new Ldap(settings)

  val ou: String = "ou=People"
  val cn: String = "cn"
  val sn: String = "sn"
  val objectClass: String = "objectclass"
  val telephoneNumber: String = "telephoneNumber"
  val person: String = "person"
}
