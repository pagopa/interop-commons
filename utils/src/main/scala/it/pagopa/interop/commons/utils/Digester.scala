package it.pagopa.interop.commons.utils

import io.circe.Json
import org.apache.commons.codec.digest.DigestUtils

import scala.xml.Elem

trait Digester[A] {
  def toSha256(value: A): String
}

object Digester {
  def toSha256[A](value: A)(implicit digester: Digester[A]): String = digester.toSha256(value)

  implicit def jsonDigester: Digester[Json] = new Digester[Json] {
    override def toSha256(value: Json): String = DigestUtils.sha256Hex(value.noSpacesSortKeys)
  }
  implicit def elemDigester: Digester[Elem] = new Digester[Elem] {
    override def toSha256(value: Elem): String = DigestUtils.sha256Hex(value.text)
  }
}
