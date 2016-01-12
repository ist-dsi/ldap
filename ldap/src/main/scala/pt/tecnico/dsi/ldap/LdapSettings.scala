package pt.tecnico.dsi.ldap

import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class LdapSettings(config: Config = ConfigFactory.load()) {
  private val path = "ldap"

  private val sslConfigs = config.getConfig(s"$path.ssl")
  private val poolConfigs = config.getConfig(s"$path.pool")

  val host: String = config.getString(s"$path.host")
  val baseDomain: String = config.getString(s"$path.base-domain")
  val bindDN: String = config.getString(s"$path.bind-dn")
  val bindPassword: String = config.getString(s"$path.bind-password")

  val connectionTimeout: FiniteDuration = config.getDuration("connection-timeout", TimeUnit.MINUTES).minutes
  val responseTimeout: FiniteDuration = config.getDuration("response-timeout", TimeUnit.MINUTES).minutes

  val blockWaitTime: FiniteDuration = poolConfigs.getDuration("block-wait-time", TimeUnit.MINUTES).minutes
  val minPoolSize: Int = poolConfigs.getInt("min-pool-size")
  val maxPoolSize: Int = poolConfigs.getInt("max-pool-size")
  val validationPeriod: FiniteDuration = poolConfigs.getDuration("validation-period", TimeUnit.MINUTES).minutes
  val prunePeriod: FiniteDuration = poolConfigs.getDuration("prune-period", TimeUnit.MINUTES).minutes
  val pruneIdleTime: FiniteDuration = poolConfigs.getDuration("prude-idle-time", TimeUnit.MINUTES).minutes

  val enableSSL: Boolean = sslConfigs.getBoolean("enable-ssl")

  // This verifies that the Config is sane and has our reference config. Importantly, we specify the "path"
  // path so we only validate settings that belong to this library.
  config.checkValid(ConfigFactory.defaultReference(), path)

}
