package it.pagopa.interop.commons.riskanalysis.model.riskAnalysisRules

import it.pagopa.interop.commons.riskanalysis.model.riskAnalysisTemplate.DataType

final case class ValidationEntry(
  fieldName: String,
  dataType: DataType,
  required: Boolean,
  dependencies: Seq[DependencyEntry],
  allowedValues: Option[Set[String]]
)
