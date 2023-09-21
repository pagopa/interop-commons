package it.pagopa.interop.commons.riskanalysis.error

import it.pagopa.interop.commons.riskanalysis.model._

object RiskAnalysisTemplateErrors {
  final case class FormTemplateConfigNotFound(templateVersion: String)
      extends Throwable(s"Config version $templateVersion not found")

  final case class TenantKindTemplateConfigNotFound(tenantKind: RiskAnalysisTenantKind)
      extends Throwable(s"Config for Tenant Kind $tenantKind not found")

  final case class IncompatibleConfig(questionId: String, configId: String)
      extends Throwable(s"Question $questionId not compatible with config $configId")

  final case class UnexpectedEmptyAnswer(questionId: String)
      extends Throwable(s"Unexpected empty answer for $questionId")

  final case class AnswerNotFoundInConfig(questionId: String, configId: String)
      extends Throwable(s"Answer $questionId not found in config $configId")

  final case class UnexpectedQuestionType(questionType: String)
      extends Throwable(s"Unexpected question type in template: $questionType")

}
