package it.pagopa.pdnd.interop.commons.jwt.errors

final case object PurposeIdNotProvided extends Throwable("purposeId claim does not exist in this assertion")
