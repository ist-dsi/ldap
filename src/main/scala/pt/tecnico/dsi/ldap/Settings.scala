package pt.tecnico.dsi.ldap

import java.security.{KeyStore, SecureRandom}
import java.time.Duration
import javax.net.ssl.SSLContext

import com.typesafe.config.{Config, ConfigFactory}
import com.unboundid.util.ssl.{SSLUtil, TrustStoreTrustManager}
import org.ldaptive._
import org.ldaptive.pool._
import org.ldaptive.provider.unboundid.{UnboundIDProvider, UnboundIDProviderConfig}
import org.ldaptive.ssl.{KeyStoreCredentialConfig, SslConfig}
import pt.tecnico.dsi.ldap.security.provider.MathsProvider

import scala.collection.JavaConverters._

class Settings(config: Config = ConfigFactory.load()) {
  // This verifies that the Config is sane and has our reference config. Importantly, we specify the "path"
  // path so we only validate settings that belong to this library.
  private val ldapConfig = {
    val reference = ConfigFactory.defaultReference()
    val finalConfig = config.withFallback(reference)
    finalConfig.checkValid(reference, "ldap")
    finalConfig.getConfig("ldap")
  }

  private val sslConfigs = ldapConfig.getConfig("ssl")
  private val poolConfigs = ldapConfig.getConfig("pool")
  private val searchConfigs = ldapConfig.getConfig("search")

  val host: String = ldapConfig.getString("host")
  val baseDomain: String = ldapConfig.getString("base-dn")
  val bindDN: String = ldapConfig.getString("bind-dn")
  val bindPassword: String = ldapConfig.getString("bind-password")

  val connectionTimeout: Duration = ldapConfig.getDuration("connection-timeout")
  val responseTimeout: Duration = ldapConfig.getDuration("response-timeout")

  val enablePool: Boolean = poolConfigs.getBoolean("enable-pool")
  val blockWaitTime: Duration = poolConfigs.getDuration("block-wait-time")
  val minPoolSize: Int = poolConfigs.getInt("min-pool-size")
  val maxPoolSize: Int = poolConfigs.getInt("max-pool-size")
  val validationPeriod: Duration = poolConfigs.getDuration("validation-period")
  val prunePeriod: Duration = poolConfigs.getDuration("prune-period")
  val pruneIdleTime: Duration = poolConfigs.getDuration("prune-idle-time")

  val enableSSL: Boolean = sslConfigs.getBoolean("enable-ssl")
  val trustStore: String = sslConfigs.getString("trust-store")
  val trustStorePassword: String = sslConfigs.getString("trust-store-password")
  val protocol: String = sslConfigs.getString("protocol")
  val enabledAlgorithms: Seq[String] = sslConfigs.getStringList("enabled-algorithms").asScala
  val randomNumberGeneratorAlgorithm: String = sslConfigs.getString("random-number-generator")

  private val credential: Credential = new Credential(bindPassword)

  val connectionConfig = new ConnectionConfig(host)
  connectionConfig.setConnectTimeout(connectionTimeout)
  connectionConfig.setResponseTimeout(responseTimeout)
  connectionConfig.setUseStartTLS(false)
  connectionConfig.setUseSSL(enableSSL)
  connectionConfig.setConnectionInitializer(new BindConnectionInitializer(bindDN, credential))

  val keyStoreConfig = new KeyStoreCredentialConfig()
  keyStoreConfig.setTrustStore(s"file:/$trustStore")
  keyStoreConfig.setTrustStorePassword(trustStorePassword)
  keyStoreConfig.setTrustStoreType(KeyStore.getDefaultType)

  val sslConfig = new SslConfig()
  sslConfig.setCredentialConfig(keyStoreConfig)
  sslConfig.setEnabledProtocols(protocol)
  sslConfig.setEnabledCipherSuites(enabledAlgorithms: _*)

  private val provider = new UnboundIDProvider()
  private val providerConfig = new UnboundIDProviderConfig()

  if (enableSSL) {
    val randomNumberGenerator = {
      val rng = randomNumberGeneratorAlgorithm match {
        case r @ ("AES128CounterSecureRNG" | "AES256CounterSecureRNG") =>
          SecureRandom.getInstance(r, MathsProvider)
        case s @ ("SHA1PRNG" | "NativePRNG") =>
          // SHA1PRNG needs /dev/urandom to be the source on Linux to prevent problems with /dev/random blocking
          // However, this also makes the seed source insecure as the seed is reused to avoid blocking (not a problem on FreeBSD).
          SecureRandom.getInstance(s)
        case _ =>
          new SecureRandom
      }
      rng.nextInt() // prevent stall on first access
      rng
    }

    connectionConfig.setSslConfig(sslConfig)
    val sslUtil = new SSLUtil(new TrustStoreTrustManager(trustStore, trustStorePassword.toCharArray, KeyStore.getDefaultType, true))

    val sslContext = SSLContext.getInstance(protocol)
    sslContext.init(sslUtil.getKeyManagers, sslUtil.getTrustManagers, randomNumberGenerator)

    providerConfig.setSSLSocketFactory(sslContext.getSocketFactory)
    provider.setProviderConfig(providerConfig)
  }

  val defaultConnectionFactory: DefaultConnectionFactory = new DefaultConnectionFactory(connectionConfig, provider)

  val poolConfig: PoolConfig = new PoolConfig()
  poolConfig.setMinPoolSize(minPoolSize)
  poolConfig.setMaxPoolSize(maxPoolSize)
  poolConfig.setValidatePeriodically(true)
  poolConfig.setValidatePeriod(validationPeriod)

  //TODO Say on documentations that if one wants to use a non blocking connection pool to extend this class an override
  //     the proper val
  val pool = new BlockingConnectionPool()
//  val pool = new SoftLimitConnectionPool()
  pool.setBlockWaitTime(blockWaitTime)
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
  val searchTimeLimit: Duration = searchConfigs.getDuration("time-limit")
}
