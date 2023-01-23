package it.pagopa.interop.commons.parser

import io.circe.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source
import scala.xml.Elem

class InterfaceParserUtilsSpec extends AnyWordSpecLike with Matchers {

  "InterfaceParserUtils" should {
    "extract urls from an Openapi 3 JSON correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_3.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 2 JSON correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_2.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("petstore.swagger.io"))
    }

    "extract urls from an Openapi 3 YAML correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_3.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 2 YAML correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_2.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("petstore.swagger.io"))
    }

    "extract urls from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Elem])

      result shouldBe Right(List("https://host.com/TestWS/v1"))
    }

    "extract endpoints from an Openapi JSON correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_3.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi YAML correctly" in {
      val bytes: Array[Byte]                      = Source.fromResource("api_3.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Elem])

      result shouldBe Right(List("http://host/TestWS/One", "http://host/TestWS/Two"))
    }
  }
}
