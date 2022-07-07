package it.pagopa.interop.commons.files

import de.redsix.pdfcompare.{CompareResult, PdfComparator}
import it.pagopa.interop.commons.files.model.PDFConfiguration
import it.pagopa.interop.commons.files.service.PDFManager
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import java.nio.file.Files
import scala.io.Source
import scala.util.Success
import de.redsix.pdfcompare.CompareResultImpl

class PDFManagerSpec extends AnyWordSpecLike with Matchers {

  val path        = getClass.getResource(s"/pdf-template-html.txt").getPath
  val pdfTemplate = Source.fromFile(new File(path)).mkString
  val expectedPDF = new File(getClass.getResource(s"/mock-generated.pdf").getPath)

  "PDFManager should generate PDF" should {

    "generating PDF as file" in {
      val data =
        Map(
          "institutionName" -> "Comune di Milano",
          "institution"     -> "Scighera enterprise",
          "users"           -> "Donatone Braghetti"
        )

      val generatedPDF: File = File.createTempFile("output", "pdf")
      PDFManager.getPDFAsFile(generatedPDF.toPath, pdfTemplate, data) shouldBe a[Success[_]]

      val result: CompareResult = new PdfComparator[CompareResultImpl](expectedPDF, generatedPDF).compare()
      result.isEqual shouldBe true

      generatedPDF.deleteOnExit()
    }

    "generating PDF as array of bytes" in {
      val data =
        Map(
          "institutionName" -> "Comune di Milano",
          "institution"     -> "Scighera enterprise",
          "users"           -> "Donatone Braghetti"
        )

      val generatedPDF: File = File.createTempFile("byte-array-output", "pdf")
      Files.write(generatedPDF.toPath, PDFManager.getPDFAsByteArray(pdfTemplate, data, PDFConfiguration.empty).get)

      val result: CompareResult = new PdfComparator[CompareResultImpl](expectedPDF, generatedPDF).compare()
      result.isEqual shouldBe true
      generatedPDF.deleteOnExit()
    }
  }

  "generating PDF as file with different field" in {
    val data =
      Map("institutionName" -> "Comune di Sestri Levante", "institution" -> "Nebbiolina", "users" -> "Mario Verdi")

    val generatedPDF: File = File.createTempFile("output", "pdf")
    PDFManager.getPDFAsFile(generatedPDF.toPath, pdfTemplate, data) shouldBe a[Success[_]]

    val result: CompareResult = new PdfComparator[CompareResultImpl](expectedPDF, generatedPDF).compare()
    result.isEqual shouldBe false

    generatedPDF.deleteOnExit()
  }

}
