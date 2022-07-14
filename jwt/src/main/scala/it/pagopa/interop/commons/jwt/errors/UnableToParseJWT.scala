package it.pagopa.interop.commons.jwt.errors

final case class UnableToParseJWT(message: String) extends Throwable(s"Unable to parse JWT - $message")
