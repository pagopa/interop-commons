package it.pagopa.interop.commons.jwt.errors

final case class InvalidPurposeIdFormat(purposeId: String)
    extends Throwable(s"Unexpected format for Purpose Id claim value $purposeId")
