package it.pagopa.pdnd.interop.commons.jwt.errors

final case class PublicKeyNotFound(message: String) extends Throwable(s"Public key error: $message")
