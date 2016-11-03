package pt.tecnico.dsi.ldap

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{AsyncFlatSpec, Matchers, OptionValues, ParallelTestExecution}

abstract class UnitSpec extends AsyncFlatSpec with Matchers with OptionValues with ParallelTestExecution
  with LazyLogging {

  val simpleLdap = new Ldap()
  val ldapWithBlockingPool = new Ldap(new Settings(ConfigFactory.parseString(s"""
  ldap {
    host = "ldap://localhost:8389"
    base-dn = "dc=example,dc=org"
    bind-dn = "cn=admin,dc=example,dc=org"
    bind-password = "admin"

    connection-timeout = 10 seconds
    response-timeout = 10 seconds

    pool {
      enable-pool = true
      min-pool-size = 1
      max-pool-size = 1
    }
  }
  """)))

  val ou: String = "ou=People"
  val cn: String = "cn"
  val sn: String = "sn"
  val doe: String = "Doe"
  val objectClass: String = "objectclass"
  val telephoneNumber: String = "telephoneNumber"
  val person: String = "person"
}
