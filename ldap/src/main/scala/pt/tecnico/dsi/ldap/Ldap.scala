package pt.tecnico.dsi.ldap

import org.ldaptive.provider.unboundid.UnboundIDProvider
import org.ldaptive.ssl.{SslConfig, KeyStoreCredentialConfig}
import org.ldaptive.{Credential, BindConnectionInitializer, DefaultConnectionFactory, ConnectionConfig}

class Ldap(settings: LdapSettings) {

  private val connectionFactory: DefaultConnectionFactory = {
    val connectionConfig = new ConnectionConfig(settings.host)
    connectionConfig.setConnectTimeout(settings.connectionTimeout)
    connectionConfig.setResponseTimeout(settings.responseTimeout)
    connectionConfig.setUseStartTLS(false)
    connectionConfig.setUseSSL(settings.enableSSL)
    connectionConfig.setConnectionInitializer(
      new BindConnectionInitializer(settings.bindDN, new Credential(settings.bindPassword)))

    if (settings.enableSSL) {
      val keyStoreConfig = new KeyStoreCredentialConfig()
      keyStoreConfig.setTrustStore(settings.trustStore)
      keyStoreConfig.setTrustStorePassword(settings.trustStorePassword)
      keyStoreConfig.setTrustStoreType(settings.trustStoreType)

      val sslConfig = new SslConfig()
      sslConfig.setCredentialConfig(keyStoreConfig)
      sslConfig.setEnabledProtocols(settings.protocol)
      sslConfig.setEnabledCipherSuites(settings.ciphers)

      connectionConfig.setSslConfig(sslConfig)
    }

    new DefaultConnectionFactory(connectionConfig, new UnboundIDProvider())
  }
}
