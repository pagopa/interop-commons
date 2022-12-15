package it.pagopa.interop.commons.parser.errors

object Errors {
  object InterfaceExtractingInfoError
      extends Throwable(s"Error trying to extract some information from interface document")
}
