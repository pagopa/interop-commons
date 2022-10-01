package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

import java.io.{File, FileInputStream, PrintWriter}
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.OffsetDateTime
import java.util.{Base64, UUID}
import scala.util.{Failure, Success, Try}

/** Defines implicit Spray JSON formats definitions for common types used through Interop platform
  */
object SprayCommonFormats {

  /** Defines the JsonFormat for <code>UUID</code> data type
    */
  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s)  =>
          Try(UUID.fromString(s)) match {
            case Success(result)    => result
            case Failure(exception) => deserializationError(s"could not parse $s as UUID", exception)
          }
        case notAJsString => deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  /** Defines the JsonFormat for <code>File</code> data type
    */
  implicit val fileFormat: JsonFormat[File] =
    new JsonFormat[File] {
      override def write(obj: File): JsValue = {
        val source = new FileInputStream(obj)
        val bytes  = source.readAllBytes()
        val base64 = Base64.getEncoder.encodeToString(bytes)
        source.close()
        JsString(base64)
      }

      override def read(json: JsValue): File = json match {
        case JsString(s)  =>
          Try {
            val file = Files.createTempFile(UUID.randomUUID().toString, ".temp").toFile
            val pw   = new PrintWriter(file, StandardCharsets.UTF_8)
            pw.write(s)
            pw.close()
            file
          } match {
            case Success(result)    => result
            case Failure(exception) => deserializationError(s"could not parse $s as File", exception)
          }
        case notAJsString => deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  /** Defines the JsonFormat for <code>OffsetDateTime</code> data type
    */
  implicit val offsetDateTimeFormat: JsonFormat[OffsetDateTime] =
    new JsonFormat[OffsetDateTime] {
      override def write(obj: OffsetDateTime): JsValue = JsString(obj.format(dateFormatter))

      override def read(json: JsValue): OffsetDateTime = json match {
        case JsString(s)  =>
          s.toOffsetDateTime match {
            case Success(result)    => result
            case Failure(exception) => deserializationError(s"could not parse $s as java OffsetDateTime", exception)
          }
        case notAJsString => deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  /** Defines the JsonFormat for <code>URI</code> data type
    */
  implicit val uriFormat: JsonFormat[URI] =
    new JsonFormat[URI] {
      override def write(obj: URI): JsValue = JsString(obj.toString)

      override def read(json: JsValue): URI = json match {
        case JsString(s)  =>
          Try(URI.create(s)) match {
            case Success(result)    => result
            case Failure(exception) => deserializationError(s"could not parse $s as URI", exception)
          }
        case notAJsString => deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }
}
