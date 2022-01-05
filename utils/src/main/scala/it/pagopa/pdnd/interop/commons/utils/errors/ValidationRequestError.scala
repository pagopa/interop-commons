package it.pagopa.pdnd.interop.commons.utils.errors

final case class ValidationRequestError(errorMessage: String) extends ComponentError("0000", errorMessage)
