package it.pagopa.interop.commons.files.model

final case class PDFFontConfig(filePath: String, familyName: String)
final case class PDFConfiguration(fonts: List[PDFFontConfig])
object PDFConfiguration {
  val empty: PDFConfiguration = PDFConfiguration(fonts = List.empty)
}
