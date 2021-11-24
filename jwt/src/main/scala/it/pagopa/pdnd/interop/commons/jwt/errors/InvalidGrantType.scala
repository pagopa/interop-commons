package it.pagopa.pdnd.interop.commons.jwt.errors

final case class InvalidGrantType(message: String) extends Throwable(s"Invalid grant type: $message")
