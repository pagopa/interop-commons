package it.pagopa.pdnd.interop.commons.errors

final case class InvalidAccessTokenRequest(errors: List[String]) extends Throwable("Invalid access token request")
