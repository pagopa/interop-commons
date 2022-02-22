package it.pagopa.interop.commons.jwt.errors

final case class InvalidSubject(subject: String)
    extends Throwable(s"Subject claim value $subject does not correspond to current provided client_id parameter")
