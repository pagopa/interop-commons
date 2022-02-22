package it.pagopa.interop.commons.jwt.errors

final case class PrivateKeyNotFound(message: String) extends Throwable(s"Private key error: $message")
