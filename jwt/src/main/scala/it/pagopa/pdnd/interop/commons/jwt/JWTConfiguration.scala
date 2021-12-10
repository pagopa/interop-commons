package it.pagopa.pdnd.interop.commons.jwt

import com.typesafe.config.{Config, ConfigFactory}

/** Defines the configuration parameters for JWT module
  */
final object JWTConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns the jwtReader instance for loading public keys
    */
  lazy val jwtReader =
    JWTWellKnownReader(
      url = config.getString("pdnd-interop-commons.jwt.public-keys.url"),
      connectTimeout = config.getInt("pdnd-interop-commons.jwt.public-keys.connection-timeout"),
      readTimeout = config.getInt("pdnd-interop-commons.jwt.public-keys.read-timeout"),
      sizeLimit = config.getInt("pdnd-interop-commons.jwt.public-keys.size-limit")
    )
}
