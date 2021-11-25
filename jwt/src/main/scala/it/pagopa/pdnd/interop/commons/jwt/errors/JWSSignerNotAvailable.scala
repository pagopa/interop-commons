package it.pagopa.pdnd.interop.commons.jwt.errors

final case class JWSSignerNotAvailable(message: String) extends Throwable(s"JWS Signer not available: $message")
