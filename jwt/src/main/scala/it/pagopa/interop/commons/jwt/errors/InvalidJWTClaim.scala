package it.pagopa.interop.commons.jwt.errors

final case class InvalidJWTClaim(message: String) extends Throwable(s"Invalid JWT claims - $message")
