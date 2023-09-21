package it.pagopa.interop.commons.riskanalysis.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.commons.riskanalysis.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def riskAnalysisFormFormat: RootJsonFormat[RiskAnalysisForm] = jsonFormat2(RiskAnalysisForm)

}
