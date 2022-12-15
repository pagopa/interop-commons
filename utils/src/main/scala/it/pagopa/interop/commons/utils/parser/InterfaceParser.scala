package it.pagopa.interop.commons.utils.parser

import io.circe.Json

import java.nio.charset.StandardCharsets
import scala.util.Try
import scala.xml.Elem
import scala.xml.XML.loadString
import io.circe.jawn.{parse => parseJson}
import io.circe.yaml.parser.{parse => parseYaml}

object InterfaceParser {

  def parseOpenApi(bytes: Array[Byte]): Either[Throwable, Json] = {
    val txt: String = new String(bytes, StandardCharsets.UTF_8)
    parseYaml(txt) orElse parseJson(txt)
  }

  def parseSoap(bytes: Array[Byte]): Either[Throwable, Elem] = {
    val txt: String = new String(bytes, StandardCharsets.UTF_8)
    Try(loadString(txt)).toEither
  }

}
