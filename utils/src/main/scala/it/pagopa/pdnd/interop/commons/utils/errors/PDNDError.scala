package it.pagopa.pdnd.interop.commons.utils.errors

/** Base model of PDND Interop errors
  * @param message - text message to be thrown
  * @param code - unique code of the error
  */
abstract case class PDNDError(message: String, code: String) extends Throwable(message)
