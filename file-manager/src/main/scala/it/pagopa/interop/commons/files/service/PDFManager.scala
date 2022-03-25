package it.pagopa.interop.commons.files.service

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import com.openhtmltopdf.slf4j.Slf4jLogger
import it.pagopa.interop.commons.utils.model.TextTemplate
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, FileOutputStream, OutputStream}
import java.nio.file.Path
import scala.util.{Try, Using}

/** Manages PDF creation from HTML templates
  */
trait PDFManager {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Defines a PDF to be streamed to a specific resource
    * @param htmlTemplate HTML template to be rendered as PDF
    * @param customData Map of data to be replaced in the template
    * @param streamOp defines a function to be performed on the output stream once the PDF has been streamed on it
    * @tparam O output stream
    * @tparam T result type (e.g.: byte array, file, etc).
    * @return
    */
  def getPDF[O <: OutputStream, T](htmlTemplate: String, customData: Map[String, String])(streamOp: O => T) = { o: O =>
    Using(o) { stream =>
      logger.debug("Getting PDF for HTML template...")
      val compiledHTML = TextTemplate(htmlTemplate, customData).toText
      val doc          = Jsoup.parse(compiledHTML, "UTF-8")
      val dom          = W3CDom.convert(doc)
      val builder      = new PdfRendererBuilder
      builder.useFastMode
      builder.withW3cDocument(dom, null)
      builder.toStream(stream)
      builder.run()
      logger.debug("PDF stream properly retrieved")
      streamOp(stream)
    }
  }

  /** Returns a PDF materialized as a byte array
    * @param htmlTemplate HTML template of the PDF
    * @param customData Map of data to be replaced in the template
    * @return array of byte representing the PDF
    */
  def getPDFAsByteArray(htmlTemplate: String, customData: Map[String, String]): Try[Array[Byte]] = {
    def toByteArray =
      getPDF[ByteArrayOutputStream, Array[Byte]](htmlTemplate, customData) { stream => stream.toByteArray }
    toByteArray(new ByteArrayOutputStream())
  }

  /** Returns a <code>File</code> reference to a generated PDF
    * @param htmlTemplate HTML template of the PDF
    * @param customData Map of data to be replaced in the template
    * @return array of byte representing the PDF
    */
  def getPDFAsFile(destination: Path, htmlTemplate: String, customData: Map[String, String]): Try[File] = {
    def toFile =
      getPDF[FileOutputStream, Unit](htmlTemplate, customData) { _ => () }
    toFile(new FileOutputStream(destination.toFile)).map(_ => destination.toFile)
  }

}

object PDFManager extends PDFManager
