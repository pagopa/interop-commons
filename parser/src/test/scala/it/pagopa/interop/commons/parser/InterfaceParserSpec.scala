package it.pagopa.interop.commons.parser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source

class InterfaceParserSpec extends AnyWordSpecLike with Matchers {

  "InterfaceParser" should {
    "parse an Openapi JSON correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_3.0.0.json").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

    "parse an Openapi YAML correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api_3.0.0.yaml").getLines().mkString("\n").getBytes
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
