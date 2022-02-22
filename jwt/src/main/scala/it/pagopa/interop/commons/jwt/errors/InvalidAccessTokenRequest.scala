package it.pagopa.interop.commons.jwt.errors

final case class InvalidAccessTokenRequest(errors: List[String])
    extends Throwable(s"Invalid access token request: ${errors.mkString(",")}")
