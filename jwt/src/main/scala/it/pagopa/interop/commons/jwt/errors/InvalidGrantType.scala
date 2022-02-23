package it.pagopa.interop.commons.jwt.errors

final case class InvalidGrantType(grantType: String) extends Throwable(s"Invalid grant type: $grantType")
