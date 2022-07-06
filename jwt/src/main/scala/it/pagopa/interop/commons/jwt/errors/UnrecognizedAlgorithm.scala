package it.pagopa.interop.commons.jwt.errors

final case class UnrecognizedAlgorithm(message: String) extends Throwable(s"Unrecognized algorithm: $message")
