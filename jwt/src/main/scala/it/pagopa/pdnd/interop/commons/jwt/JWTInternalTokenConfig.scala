package it.pagopa.pdnd.interop.commons.jwt

final case class JWTInternalTokenConfig(
  issuer: String,
  subject: String,
  audience: Set[String],
  durationInMilliseconds: Long
)
