package it.pagopa.interop.commons.riskanalysis.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

sealed trait RiskAnalysisAnswer

final case class RiskAnalysisForm(version: String, answers: Map[String, Seq[String]])
final case class RiskAnalysisMultiAnswerValidated(key: String, values: Seq[String])           extends RiskAnalysisAnswer
final case class RiskAnalysisSingleAnswerValidated(key: String, value: Option[String] = None) extends RiskAnalysisAnswer
final case class RiskAnalysisFormSeed(
  version: String,
  singleAnswers: Seq[RiskAnalysisSingleAnswerValidated],
  multiAnswers: Seq[RiskAnalysisMultiAnswerValidated]
) extends RiskAnalysisAnswer

sealed trait RiskAnalysisTenantKind

object RiskAnalysisTenantKind {
  case object PA      extends RiskAnalysisTenantKind
  case object GSP     extends RiskAnalysisTenantKind
  case object PRIVATE extends RiskAnalysisTenantKind
}

object RiskAnalysis extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def riskAnalysisFormFormat: RootJsonFormat[RiskAnalysisForm] = jsonFormat2(RiskAnalysisForm)
}
