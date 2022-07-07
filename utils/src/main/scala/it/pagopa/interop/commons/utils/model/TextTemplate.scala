package it.pagopa.interop.commons.utils.model
import it.pagopa.interop.commons.utils.TypeConversions.StringOps

/** Defines a customizable text template through strings interpolation.<br>
  * Expected syntax for interpolation variables is: <code>\${variable}</code>
  *
  * @param text      string representing a text template
  * @param variables variables to be replaced in the template, empty by default
  */
final case class TextTemplate(text: String, variables: Map[String, String] = Map.empty) {
  def toText: String = text interpolate variables
}
