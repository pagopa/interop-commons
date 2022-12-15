package it.pagopa.interop.commons.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import scala.io.Source

class DigesterSpec extends AnyWordSpecLike with Matchers {

  "Digester" should {
    "produce SHA256 string from Array[Bytes]" in {
      val bytes: Array[Byte] = Source.fromResource("file.txt").getLines().mkString.getBytes
      val result: String     = Digester.toSha256[Array[Byte]](bytes)

      result shouldBe "f3f765d9173ac940684293aff69b1170e6543b289d5df03833add37aaf4927ba"
    }

    "produce SHA256 string from File" in {
      val file: File     = new File(getClass.getResource("/file.txt").toURI)
      val result: String = Digester.toSha256(file)

      result shouldBe "f3f765d9173ac940684293aff69b1170e6543b289d5df03833add37aaf4927ba"
    }

    "produce MD5 string from Array[Bytes]" in {
      val bytes: Array[Byte] = Source.fromResource("file.txt").getLines().mkString.getBytes
      val result: String     = Digester.toMD5[Array[Byte]](bytes)

      result shouldBe "97ad329baa7704152feeb9e72067e9a4"
    }

    "produce MD5 string from File" in {
      val file: File     = new File(getClass.getResource("/file.txt").toURI)
      val result: String = Digester.toMD5(file)

      result shouldBe "97ad329baa7704152feeb9e72067e9a4"
    }

  }

}
