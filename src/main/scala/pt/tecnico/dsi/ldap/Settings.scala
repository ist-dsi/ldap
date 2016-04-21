package pt.tecnico.dsi.ldap

import java.security.KeyStore
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, Config}
import org.ldaptive.pool._
import org.ldaptive.provider.unboundid.UnboundIDProvider
import org.ldaptive.ssl.{SslConfig, KeyStoreCredentialConfig}
import org.ldaptive._

class Settings(config: Config = ConfigFactory.load()) {
  private val path = "ldap"

  private val sslConfigs = config.getConfig(s"$path.ssl")
  private val poolConfigs = config.getConfig(s"$path.pool")
  private val searchConfigs = config.getConfig(s"$path.search")

  val host: String = config.getString(s"$path.host")
  val baseDomain: String = config.getString(s"$path.base-dn")
  val bindDN: String = config.getString(s"$path.bind-dn")
  val bindPassword: String = config.getString(s"$path.bind-password")

  val connectionTimeout: Long = config.getDuration("connection-timeout", TimeUnit.MILLISECONDS)
  val responseTimeout: Long = config.getDuration("response-timeout", TimeUnit.MILLISECONDS)

  val enablePool: Boolean = poolConfigs.getBoolean("enable-pool")
  val blockWaitTime: Long = poolConfigs.getDuration("block-wait-time", TimeUnit.MILLISECONDS)
  val minPoolSize: Int = poolConfigs.getInt("min-pool-size")
  val maxPoolSize: Int = poolConfigs.getInt("max-pool-size")
  val validationPeriod: Long = poolConfigs.getDuration("validation-period", TimeUnit.MILLISECONDS)
  val prunePeriod: Long = poolConfigs.getDuration("prune-period", TimeUnit.MILLISECONDS)
  val pruneIdleTime: Long = poolConfigs.getDuration("prude-idle-time", TimeUnit.MILLISECONDS)

  val enableSSL: Boolean = sslConfigs.getBoolean("enable-ssl")
  val trustStore: String = sslConfigs.getString("trust-store")
  val trustStorePassword: String = sslConfigs.getString("trust-store-password")
  val protocol: String = sslConfigs.getString("protocol")
  val enabledAlgorithms: String = sslConfigs.getString("enabled-algorithms")

  // This verifies that the Config is sane and has our reference config. Importantly, we specify the "path"
  // path so we only validate settings that belong to this library.
  config.checkValid(ConfigFactory.defaultReference(), path)

  private val credential: Credential = new Credential(bindPassword)

  val connectionConfig = new ConnectionConfig(host)
  connectionConfig.setConnectTimeout(connectionTimeout)
  connectionConfig.setResponseTimeout(responseTimeout)
  connectionConfig.setUseStartTLS(false)
  connectionConfig.setUseSSL(enableSSL)
  connectionConfig.setConnectionInitializer(new BindConnectionInitializer(bindDN, credential))

  val keyStoreConfig = new KeyStoreCredentialConfig()
  keyStoreConfig.setTrustStore(trustStore)
  keyStoreConfig.setTrustStorePassword(trustStorePassword)
  keyStoreConfig.setTrustStoreType(KeyStore.getDefaultType)

  val sslConfig = new SslConfig()
  sslConfig.setCredentialConfig(keyStoreConfig)
  sslConfig.setEnabledProtocols(protocol)
  sslConfig.setEnabledCipherSuites(enabledAlgorithms)

  if (enableSSL) {
    connectionConfig.setSslConfig(sslConfig)
  }

  val defaultConnectionFactory: DefaultConnectionFactory = new DefaultConnectionFactory(connectionConfig,
    new UnboundIDProvider())

  val poolConfig: PoolConfig = new PoolConfig()
  poolConfig.setMinPoolSize(minPoolSize)
  poolConfig.setMaxPoolSize(maxPoolSize)
  poolConfig.setValidatePeriodically(true)
  poolConfig.setValidatePeriod(validationPeriod)

  val pool: AbstractConnectionPool = new SoftLimitConnectionPool()
  pool.setConnectionFactory(defaultConnectionFactory)
  pool.setFailFastInitialize(true)
  //Before connections are checked back into the pool a bind request will be made.
  //This makes connections consistent with ConnectionInitializer from ConnectionConfig
  pool.setPassivator(new BindPassivator(new BindRequest(bindDN, credential)))
  pool.setPoolConfig(poolConfig)
  //SearchValidator - connection is valid if the search operation returns one or more results.
  //Connections that fail validation are evicted from the pool.
  pool.setValidator(new SearchValidator())
  //Prunes connections from the pool based on how long they have been idle.
  pool.setPruneStrategy(new IdlePruneStrategy(prunePeriod, pruneIdleTime))

  val pooledConnectionFactory: PooledConnectionFactory = new PooledConnectionFactory(pool)

  val searchDereferenceAlias: String = searchConfigs.getString("dereference-alias")
  val searchScope: String = searchConfigs.getString("scope")
  val searchSizeLimit: Int = searchConfigs.getInt("size-limit")
  val searchTimeLimit: Long = config.getDuration("time-limit", TimeUnit.MILLISECONDS)
}
