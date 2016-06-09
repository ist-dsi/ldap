package pt.tecnico.dsi.ldap.security.provider

import java.security.{PrivilegedAction, AccessController, Provider}

/**
  * A provider that for AES128CounterRNGFast, a cryptographically secure random number generator through SecureRandom
  */
object MathsProvider extends Provider("UncommonsMath", 1.0, "uncommons.math provider 1.0 that implements a secure AES random number generator, for " +
  "uncommons.math based on Akka Implementation") {
  AccessController.doPrivileged(new PrivilegedAction[MathsProvider.type] {
    def run: MathsProvider.type = {
      // SecureRandom
      put("SecureRandom.AES128CounterSecureRNG", classOf[AES128CounterSecureRNG].getName)
      put("SecureRandom.AES256CounterSecureRNG", classOf[AES256CounterSecureRNG].getName)

      // Implementation type: software or hardware
      put("SecureRandom.AES128CounterSecureRNG ImplementedIn", "Software")
      put("SecureRandom.AES256CounterSecureRNG ImplementedIn", "Software")

      // As we don't want to return a value, we must return null. See https://docs.oracle.com/javase/8/docs/api/java/security/AccessController.html
      null // scalastyle:ignore null
    }
  })
}
