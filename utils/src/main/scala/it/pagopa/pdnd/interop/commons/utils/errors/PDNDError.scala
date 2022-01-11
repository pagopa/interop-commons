package it.pagopa.pdnd.interop.commons.utils.errors

/** Base model of PDND Interop errors
  * @param t - throwable message
  * @param code - unique code of the error
  */
trait PDNDError {
  t: Throwable =>
  val code: String
}

/** Models the structure of PDND component business errors
  * @param code - unique code of the error
  * @param msg - message to be thrown
  */
abstract class ComponentError(val code: String, val msg: String) extends Throwable(msg) with PDNDError
