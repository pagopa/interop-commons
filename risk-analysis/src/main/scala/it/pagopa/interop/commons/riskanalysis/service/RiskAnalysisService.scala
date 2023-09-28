package it.pagopa.interop.commons.riskanalysis.service

import it.pagopa.interop.commons.riskanalysis.model.riskAnalysisTemplate.RiskAnalysisFormConfig
import it.pagopa.interop.commons.riskanalysis.model.RiskAnalysisTenantKind

import scala.io.Source
import spray.json._

trait RiskAnalysisService {
  def riskAnalysisForms(): Map[RiskAnalysisTenantKind, Map[String, RiskAnalysisFormConfig]]
  def loadRiskAnalysisFormConfig(resourcePath: String): RiskAnalysisFormConfig
}

object RiskAnalysisService extends RiskAnalysisService {

  private val riskAnalysisTemplatePath: String = "riskAnalysisTemplate/forms"

  private val riskAnalysisFormsMap: Map[RiskAnalysisTenantKind, Map[String, RiskAnalysisFormConfig]] = Map(
    RiskAnalysisTenantKind.PA      -> Map(
      "1.0" -> loadRiskAnalysisFormConfig(s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PA.toString}/1.0.json"),
      "2.0" -> loadRiskAnalysisFormConfig(s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PA.toString}/2.0.json"),
      "3.0" -> loadRiskAnalysisFormConfig(s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PA.toString}/3.0.json")
    ),
    RiskAnalysisTenantKind.PRIVATE -> Map(
      "1.0" -> loadRiskAnalysisFormConfig(
        s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PRIVATE.toString}/1.0.json"
      ),
      "2.0" -> loadRiskAnalysisFormConfig(
        s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PRIVATE.toString}/2.0.json"
      )
    ),
    RiskAnalysisTenantKind.GSP     -> Map(
      "1.0" -> loadRiskAnalysisFormConfig(
        s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PRIVATE.toString}/1.0.json"
      ),
      "2.0" -> loadRiskAnalysisFormConfig(
        s"$riskAnalysisTemplatePath/${RiskAnalysisTenantKind.PRIVATE.toString}/2.0.json"
      )
    )
  )

  def riskAnalysisForms(): Map[RiskAnalysisTenantKind, Map[String, RiskAnalysisFormConfig]] =
    riskAnalysisFormsMap

  def loadRiskAnalysisFormConfig(resourcePath: String): RiskAnalysisFormConfig =
    Source
      .fromResource(resourcePath)
      .getLines()
      .mkString(System.lineSeparator())
      .parseJson
      .convertTo[RiskAnalysisFormConfig]

}
