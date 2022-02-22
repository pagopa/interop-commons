package it.pagopa.interop.commons.utils.errors

/** Base model of Interop errors
  * @param t - throwable message
  * @param code - unique code of the error
  */
trait InteropError {
  t: Throwable =>
  val code: String
}

/** Models the structure of Interop component business errors
  * @param code - unique code of the error
  * @param msg - message to be thrown
  */
abstract class ComponentError(val code: String, val msg: String) extends Throwable(msg) with InteropError
