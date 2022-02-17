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

  lazy val jwtInternalTokenConfig = JWTInternalTokenConfig(
    issuer = config.getString("pdnd-interop-commons.jwt.internal-token.issuer"),
    subject = config.getString("pdnd-interop-commons.jwt.internal-token.subject"),
    audience = Set(config.getString("pdnd-interop-commons.jwt.internal-token.audience")),
    durationInSeconds = config.getLong("pdnd-interop-commons.jwt.internal-token.duration-seconds")
  )

}
