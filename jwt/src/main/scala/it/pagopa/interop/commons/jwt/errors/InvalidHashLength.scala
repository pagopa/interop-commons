package it.pagopa.interop.commons.jwt.errors

final case class InvalidHashLength(alg: String) extends Throwable(s"Invalid hash length for algorithm $alg")
