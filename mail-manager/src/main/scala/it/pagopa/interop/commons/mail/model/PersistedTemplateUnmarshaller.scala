package it.pagopa.interop.commons.mail.model

import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.{Success, Try}

/** Unmarshals strings representing persisted templates.
  */
trait PersistedTemplateUnmarshaller {
  implicit val persistedTemplate: RootJsonFormat[PersistedTemplate] = jsonFormat2(PersistedTemplate)

  def toPersistedTemplate(template: String): Try[PersistedTemplate] = {
    for {
      jsValue        <- parseJson(template)
      encoded        <- isEncoded(jsValue)
      template       <- toTemplate(jsValue)
      actualTemplate <- actualTemplate(encoded, template)
    } yield actualTemplate
  }

  private def toTemplate(jsonValue: JsValue): Try[PersistedTemplate] = Try {
    jsonValue.convertTo[PersistedTemplate]
  }

  private def actualTemplate(isEncoded: Boolean, template: PersistedTemplate): Try[PersistedTemplate] = {
    if (isEncoded) {
      for {
        decodedSubject <- template.subject.decodeBase64
        decodedBody    <- template.body.decodeBase64
      } yield PersistedTemplate(subject = decodedSubject, body = decodedBody)
    } else
      Success(template)
  }

  private def parseJson(template: String): Try[JsValue] = Try {
    template.parseJson
  }

  private def isEncoded(jsonValue: JsValue): Try[Boolean] = Try {
    val json: JsObject = jsonValue.asJsObject
    json.fields
      .get("encoded")
      .map(value =>
        value match {
          case JsString(value: String) => value.toLowerCase == "true"
          case JsBoolean(true)         => true
          case _                       => false
        }
      )
      .getOrElse(false)
  }

}

object PersistedTemplateUnmarshaller extends PersistedTemplateUnmarshaller
