package it.pagopa.interop.commons.riskanalysis.model.riskAnalysisTemplate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

final case class LabeledValue(label: LocalizedText, value: String)

object LabeledValue extends DefaultJsonProtocol with SprayJsonSupport {
  implicit def format: RootJsonFormat[LabeledValue] = jsonFormat2(LabeledValue.apply)
}
