package it.pagopa.interop.commons.parser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source

class InterfaceParserSpec extends AnyWordSpecLike with Matchers {

  "InterfaceParser" should {
    "parse an Openapi 3 JSON correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_3.json").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

    "parse an Openapi 2 JSON correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_2.json").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

    "parse an Openapi 3 YAML correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_3.yaml").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

    "parse an Openapi 2 YAML correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_2.yaml").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

    "parse a WSDL correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
      InterfaceParser.parseWSDL(bytes).isRight shouldBe true
    }

    "parse a WSDL with BOM char correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_with_BOM.wsdl").getLines().mkString("\n").getBytes
      InterfaceParser.parseWSDL(bytes).isRight shouldBe true
    }
  }
}
