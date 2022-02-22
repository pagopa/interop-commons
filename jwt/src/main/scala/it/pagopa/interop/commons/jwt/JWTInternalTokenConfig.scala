package it.pagopa.interop.commons.jwt

final case class JWTInternalTokenConfig(issuer: String, subject: String, audience: Set[String], durationInSeconds: Long)
