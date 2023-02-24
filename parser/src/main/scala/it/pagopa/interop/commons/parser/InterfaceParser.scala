package it.pagopa.interop.commons.parser

import io.circe.Json

import java.nio.charset.StandardCharsets
import scala.util.Try
import io.circe.jawn.{parse => parseJson}
import io.circe.yaml.parser.{parse => parseYaml}

import scala.xml.Elem
import scala.xml.XML.loadString

object InterfaceParser {
  private final val UTF8_BOM = "\uFEFF"

  def parseOpenApi(bytes: Array[Byte]): Either[Throwable, Json] = {
    val txt: String = new String(bytes, StandardCharsets.UTF_8)
    parseYaml(txt) orElse parseJson(txt)
  }

  def parseWSDL(bytes: Array[Byte]): Either[Throwable, Elem] = {
    val txt: String        = new String(bytes, StandardCharsets.UTF_8).trim
    val withoutBOM: String = if (txt.startsWith(UTF8_BOM)) txt.substring(1) else txt
    Try(loadString(withoutBOM)).toEither
  }

}
