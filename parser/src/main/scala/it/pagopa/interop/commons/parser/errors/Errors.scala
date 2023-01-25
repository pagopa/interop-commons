package it.pagopa.interop.commons.parser.errors

object Errors {
  object InterfaceExtractingInfoError
      extends Exception(s"Error trying to extract some information from interface document")

  final case class OpenapiVersionNotRecognized(version: String)
      extends Exception(s"Openapi version not recognized - $version")
}
