package it.pagopa.interop.commons.jwt.model

final case class Token(serialized: String, jti: String, iat: Long, exp: Long, nbf: Long)
