package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.parser.InterfaceParser
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source

class InterfaceParserSpec extends AnyWordSpecLike with Matchers {

  "InterfaceParser" should {
    "parse an Openapi JSON correctly" in {
      val bytes: Array[Byte] = Source.fromResource("api.json").getLines().mkString("\n").getBytes
      InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
    }

  }

  "parse an Openapi YAML correctly" in {
    val bytes: Array[Byte] = Source.fromResource("api.yaml").getLines().mkString("\n").getBytes
    InterfaceParser.parseOpenApi(bytes).isRight shouldBe true
  }

  "parse a WSDL correctly" in {
    val bytes: Array[Byte] = Source.fromResource("api.wsdl").getLines().mkString("\n").getBytes
    InterfaceParser.parseSoap(bytes).isRight shouldBe true
  }
}
