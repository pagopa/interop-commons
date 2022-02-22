package it.pagopa.interop.commons.jwt.errors

final case object SubjectNotFound extends Throwable("Subject claim not found in this JWT.")
