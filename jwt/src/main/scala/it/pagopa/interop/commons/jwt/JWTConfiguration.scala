package it.pagopa.interop.commons.jwt

import com.typesafe.config.{Config, ConfigFactory}

/** Defines the configuration parameters for JWT module
  */
final object JWTConfiguration {
  val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns the jwtReader instance for loading public keys
    */
  lazy val jwtReader: JWTWellKnownReader =
    JWTWellKnownReader(
      urls = config.getString("interop-commons.jwt.public-keys.urls").split(",").toList.filter(_.nonEmpty),
      connectTimeout = config.getInt("interop-commons.jwt.public-keys.connection-timeout"),
      readTimeout = config.getInt("interop-commons.jwt.public-keys.read-timeout"),
      sizeLimit = config.getInt("interop-commons.jwt.public-keys.size-limit")
    )

  lazy val jwtInternalTokenConfig: JWTInternalTokenConfig = JWTInternalTokenConfig(
    issuer = config.getString("interop-commons.jwt.internal-token.issuer"),
    subject = config.getString("interop-commons.jwt.internal-token.subject"),
    audience = Set(config.getString("interop-commons.jwt.internal-token.audience")),
    durationInSeconds = config.getLong("interop-commons.jwt.internal-token.duration-seconds")
  )

}
