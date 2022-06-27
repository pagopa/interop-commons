package it.pagopa.interop.commons.files.model

// Url must begin and end with the separator "/"

/**
  * @param resourcesBaseUrl base path for assets references used inside html template
  */
final case class PDFConfiguration(resourcesBaseUrl: Option[String])

object PDFConfiguration {
  val empty: PDFConfiguration = PDFConfiguration(resourcesBaseUrl = None)
}
