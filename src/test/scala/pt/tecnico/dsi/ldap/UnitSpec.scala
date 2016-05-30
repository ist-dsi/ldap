package pt.tecnico.dsi.ldap

import org.scalatest.{FlatSpec, Matchers}

abstract class UnitSpec extends FlatSpec with Matchers {
  val settings = new Settings()
  val simpleLdap = new Ldap(settings)
}
