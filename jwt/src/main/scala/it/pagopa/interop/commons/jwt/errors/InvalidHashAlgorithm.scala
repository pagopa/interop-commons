package it.pagopa.interop.commons.jwt.errors

case object InvalidHashAlgorithm extends Throwable(s"Invalid hash algorithm")
