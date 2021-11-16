package it.pagopa.pdnd.interop.commons.mail.model

import spray.json._
import DefaultJsonProtocol._

import scala.util.Try

/** Unmarshals strings representing persisted templates.
  */
trait PersistedTemplateUnmarshaller {
  implicit val persistedTemplate: RootJsonFormat[PersistedTemplate] = jsonFormat2(PersistedTemplate)
  def toPersistedTemplate(template: String): Try[PersistedTemplate] =
    Try {
      template.parseJson.convertTo[PersistedTemplate]
    }
}

object PersistedTemplateUnmarshaller extends PersistedTemplateUnmarshaller
