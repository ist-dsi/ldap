/**
  * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
  */
package pt.tecnico.dsi.ldap.security.provider

/**
  * INTERNAL API
  * From AESCounterRNG API docs:
  * Valid values are 16 (128 bits), 24 (192 bits) and 32 (256 bits).
  * Any other values will result in an exception from the AES implementation.
  *
  * INTERNAL API
  */
private[provider] object SeedSize {
  val seed128 = 16
  val seed192 = 24
  val seed256 = 32
}
