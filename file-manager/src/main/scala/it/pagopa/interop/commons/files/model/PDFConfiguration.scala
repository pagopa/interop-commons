package it.pagopa.interop.commons.files.model

// Url must begin and end with the separator "/"
final case class PDFConfiguration(resourcesBaseUrl: Option[String])

object PDFConfiguration {
  val empty: PDFConfiguration = PDFConfiguration(resourcesBaseUrl = None)
}
