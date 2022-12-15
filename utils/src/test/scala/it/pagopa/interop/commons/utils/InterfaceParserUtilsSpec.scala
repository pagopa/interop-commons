package it.pagopa.interop.commons.utils

import io.circe.Json
import it.pagopa.interop.commons.utils.parser.{InterfaceExtractor, InterfaceParser}
import it.pagopa.interop.commons.utils.parser.InterfaceExtractor._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source
import scala.xml.Elem

class InterfaceExtractorSpec extends AnyWordSpecLike with Matchers {

  "Interface Extractor" should {
    "extract urls from an Openapi JSON correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api"))
    }

    "extract urls from an Openapi YAML correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api"))
    }

    "extract urls from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseSoap(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getUrls[Elem])

      result shouldBe Right(List("https://host.com/TestWS/v1"))
    }

    "extract endpoints from an Openapi JSON correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi YAML correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseSoap(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceExtractor.getEndpoints[Elem])

      result shouldBe Right(List("http://host/TestWS/One", "http://host/TestWS/Two"))
    }
  }
}
