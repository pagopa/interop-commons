package it.pagopa.interop.commons.jwt.errors

final case class InvalidClientAssertionType(message: String)
    extends Throwable(s"Invalid client credential type: $message")
