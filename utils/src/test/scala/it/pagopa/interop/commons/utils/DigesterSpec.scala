package it.pagopa.interop.commons.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import scala.io.Source

class DigesterSpec extends AnyWordSpecLike with Matchers {

  "Digester" should {
    "produce SHA256 string from Array[Bytes]" in {
      val bytes: Array[Byte] = Source.fromResource("api.json").getLines().mkString("\n").getBytes
      val result: String     = Digester.toSha256[Array[Byte]](bytes)

      result shouldBe "b96973ae316c64cfd61953aa5bcc98c458e073eab5fbbbf25bccb9fcbf1effae"
    }

    "produce SHA256 string from File" in {
      val file: File     = new File(getClass.getResource("/api.json").toURI)
      val result: String = Digester.toSha256(file)

      result shouldBe "b96973ae316c64cfd61953aa5bcc98c458e073eab5fbbbf25bccb9fcbf1effae"
    }

    "produce MD5 string from Array[Bytes]" in {
      val bytes: Array[Byte] = Source.fromResource("api.yaml").getLines().mkString("\n").getBytes
      val result: String     = Digester.toMD5[Array[Byte]](bytes)

      result shouldBe "10441cf74b8a685117489febf01d893b"
    }

    "produce MD5 string from File" in {
      val file: File     = new File(getClass.getResource("/api.yaml").toURI)
      val result: String = Digester.toMD5(file)

      result shouldBe "10441cf74b8a685117489febf01d893b"
    }

  }

}
