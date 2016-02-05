package pt.tecnico.dsi.ldap

import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions._

import com.typesafe.config.{ConfigFactory, Config}

class LdapSettings(config: Config = ConfigFactory.load()) {
  private val path = "ldap"

  private val reference = ConfigFactory.defaultReference()
  private val finalConfig = config.withFallback(reference)

  private val sslConfigs = config.getConfig(s"$path.ssl")
  private val poolConfigs = config.getConfig(s"$path.pool")

  // This verifies that the Config is sane and has our reference config. Importantly, we specify the "path"
  // path so we only validate settings that belong to this library.
  config.checkValid(ConfigFactory.defaultReference(), path)

  val host: String = config.getString(s"$path.host")
  val baseDomain: String = config.getString(s"$path.base-dn")
  val bindDN: String = config.getString(s"$path.bind-dn")
  val bindPassword: String = config.getString(s"$path.bind-password")

  val connectionTimeout: Long = config.getDuration(s"$path.connection-timeout", TimeUnit.MILLISECONDS)
  val responseTimeout: Long = config.getDuration(s"$path.response-timeout", TimeUnit.MILLISECONDS)

  val blockWaitTime: Long = poolConfigs.getDuration("block-wait-time", TimeUnit.MILLISECONDS)
  val minPoolSize: Int = poolConfigs.getInt("min-pool-size")
  val maxPoolSize: Int = poolConfigs.getInt("max-pool-size")
  val validationPeriod: Long = poolConfigs.getDuration("validation-period", TimeUnit.MILLISECONDS)
  val prunePeriod: Long = poolConfigs.getDuration("prune-period", TimeUnit.MILLISECONDS)
  val pruneIdleTime: Long = poolConfigs.getDuration("prune-idle-time", TimeUnit.MILLISECONDS)

  val enableSSL: Boolean = sslConfigs.getBoolean("enable-ssl")
  val trustStore: String = sslConfigs.getString("trust-store")
  val trustStorePassword: String = sslConfigs.getString("trust-store-password")
  val protocol: String = sslConfigs.getString("protocol")
  val enabledAlgorithms: Seq[String] = sslConfigs.getStringList("enabled-algorithms")

}
