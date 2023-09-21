package it.pagopa.interop.commons.riskanalysis.model

import scala.io.Source

sealed trait RiskAnalysisModel

final case class RiskAnalysisForm(version: String, answers: Map[String, Seq[String]])
final case class RiskAnalysisMultiAnswerValidated(key: String, values: Seq[String])           extends RiskAnalysisModel
final case class RiskAnalysisSingleAnswerValidated(key: String, value: Option[String] = None) extends RiskAnalysisModel
final case class RiskAnalysisFormSeed(
  version: String,
  singleAnswers: Seq[RiskAnalysisSingleAnswerValidated],
  multiAnswers: Seq[RiskAnalysisMultiAnswerValidated]
) extends RiskAnalysisModel

sealed trait RiskAnalysisTenantKind

object RiskAnalysisTenantKind {
  case object PA      extends RiskAnalysisTenantKind
  case object GSP     extends RiskAnalysisTenantKind
  case object PRIVATE extends RiskAnalysisTenantKind
}

object RiskAnalysisTemplate {
  val riskAnalysisTemplate = Source
    .fromResource("riskAnalysisTemplate/index.html")
    .getLines()
    .mkString(System.lineSeparator())
}
