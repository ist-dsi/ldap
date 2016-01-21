package pt.tecnico.dsi.ldap

import java.security.KeyStore

import com.typesafe.scalalogging.LazyLogging

import org.ldaptive.pool._
import org.ldaptive.provider.unboundid.UnboundIDProvider
import org.ldaptive.ssl.{SslConfig, KeyStoreCredentialConfig}
import org.ldaptive._

class Ldap(settings: LdapSettings) extends LazyLogging {

  private val credential: Credential = new Credential(settings.bindPassword)

  private val connectionConfig = new ConnectionConfig(settings.host)
  connectionConfig.setConnectTimeout(settings.connectionTimeout)
  connectionConfig.setResponseTimeout(settings.responseTimeout)
  connectionConfig.setUseStartTLS(false)
  connectionConfig.setUseSSL(settings.enableSSL)
  connectionConfig.setConnectionInitializer(new BindConnectionInitializer(settings.bindDN, credential))

  if (settings.enableSSL) {
    val keyStoreConfig = new KeyStoreCredentialConfig()
    keyStoreConfig.setTrustStore(settings.trustStore)
    keyStoreConfig.setTrustStorePassword(settings.trustStorePassword)
    keyStoreConfig.setTrustStoreType(KeyStore.getDefaultType)

    val sslConfig = new SslConfig()
    sslConfig.setCredentialConfig(keyStoreConfig)
    sslConfig.setEnabledProtocols(settings.protocol)
    sslConfig.setEnabledCipherSuites(settings.enabledAlgorithms)

    connectionConfig.setSslConfig(sslConfig)
  }

  private val connectionFactory: DefaultConnectionFactory = new DefaultConnectionFactory(connectionConfig,
    new UnboundIDProvider())

  private val poolConfig: PoolConfig = new PoolConfig()
  poolConfig.setMinPoolSize(settings.minPoolSize)
  poolConfig.setMaxPoolSize(settings.maxPoolSize)
  poolConfig.setValidatePeriodically(true)
  poolConfig.setValidatePeriod(settings.validationPeriod)

  private val pool: AbstractConnectionPool = new SoftLimitConnectionPool()
  pool.setConnectionFactory(connectionFactory)
  pool.setFailFastInitialize(true)
  //Before connections are checked back into the pool a bind request will be made.
  //This makes connections consistent with ConnectionInitializer from ConnectionConfig
  pool.setPassivator(new BindPassivator(new BindRequest(settings.bindDN, credential)))
  pool.setPoolConfig(poolConfig)
  //SearchValidator - connection is valid if the search operation returns one or more results.
  //Connections that fail validation are evicted from the pool.
  pool.setValidator(new SearchValidator())
  //Prunes connections from the pool based on how long they have been idle.
  pool.setPruneStrategy(new IdlePruneStrategy(settings.prunePeriod, settings.pruneIdleTime))

  implicit private val pooledConnectionFactory: PooledConnectionFactory = new PooledConnectionFactory(pool)

  def initializePool(): Unit = {
    pool.initialize()
    logger.info("Connection pool initialized successfully.")
  }
}
