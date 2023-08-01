package it.pagopa.interop.commons.parser

import io.circe.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source
import scala.xml.Elem

class InterfaceParserUtilsSpec extends AnyWordSpecLike with Matchers {

  "InterfaceParserUtils" should {

    "extract urls from an Openapi 3.1.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.1.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.3 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.3.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.2 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.2.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.1 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.1.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 2.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_2.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("petstore.swagger.io"))
    }

    "fail extracting urls from a new JSON Openapi version" in {
      val bytes: Array[Byte] = Source.fromResource("api_new_version.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result.isLeft shouldBe true
    }

    "extract urls from an Openapi 3.1.0 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.1.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.3 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.3.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.2 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.2.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.1 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.1.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 3.0.0 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("http://petstore.swagger.io/api/v1", "http://petstore.swagger.io/api/v2"))
    }

    "extract urls from an Openapi 2 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_2.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result shouldBe Right(List("petstore.swagger.io"))
    }

    "fail extracting urls from a new YAML Openapi version" in {
      val bytes: Array[Byte] = Source.fromResource("api_new_version.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Json])

      result.isLeft shouldBe true
    }

    "extract urls from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Elem])

      result shouldBe Right(List("https://host.com/TestWS/v1"))
    }

    "extract endpoints from an Openapi 3.1.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.1.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.3 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.3.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.2 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.2.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.1 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.1.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 2.0 JSON correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_2.0.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "fail extracting endpoints from a new JSON Openapi version" in {
      val bytes: Array[Byte] = Source.fromResource("api_new_version.json").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result.isLeft shouldBe true
    }

    "extract endpoints from an Openapi 3.1.0 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.1.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.3 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.3.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.2 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.2.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.1 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.1.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 3.0.0 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_3.0.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "extract endpoints from an Openapi 2.0 YAML correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api_2.0.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json] = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result shouldBe Right(List("/pets", "/pets/{id}"))
    }

    "fail extracting endpoints from a new YAML Openapi version" in {
      val bytes: Array[Byte] = Source.fromResource("api_new_version.yaml").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Json]         = InterfaceParser.parseOpenApi(bytes)
      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Json])

      result.isLeft shouldBe true
    }

    "fail extracting urls from a w/o service" in {
      val bytes: Array[Byte] = Source.fromResource("api_without_service.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getUrls[Elem])

      result.isLeft shouldBe true
    }

    "fail extracting endpoints from a w/o binding" in {
      val bytes: Array[Byte] = Source.fromResource("api_without_binding.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Elem])

      result.isLeft shouldBe true
    }

    "extract endpoints from a WSDL correctly" in {
      val bytes: Array[Byte]              = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Elem])

      result shouldBe Right(List("http://host/TestWS/One", "http://host/TestWS/Two"))
    }

    "extract endpoints from a WSDL without soapName attribute" in {
      val bytes: Array[Byte] = Source.fromResource("api_without_soapAction.wsdl").getLines().mkString("\n").getBytes
      val parsed: Either[Throwable, Elem] = InterfaceParser.parseWSDL(bytes)

      val result: Either[Throwable, List[String]] = parsed.flatMap(InterfaceParserUtils.getEndpoints[Elem])

      result shouldBe Right(List("One", "Two"))
    }
  }
}
