package it.pagopa.interop.commons.jwt.errors

final case class InvalidSubjectFormat(subject: String)
    extends Throwable(s"Unexpected format for Subject claim value $subject")
