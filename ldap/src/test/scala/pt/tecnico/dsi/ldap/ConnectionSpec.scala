package pt.tecnico.dsi.ldap

class ConnectionSpec extends UnitSpec {
  "EstablishConnection" should "succeed" in {
    new Ldap(new Settings()).duinitializePool()
  }
}
