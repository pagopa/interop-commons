package it.pagopa.interop.purposeprocess

import it.pagopa.interop.commons.riskanalysis.model._
import java.time.{OffsetDateTime, ZoneOffset}

object SpecData {

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val validRiskAnalysis3_0_Pa: RiskAnalysisForm = RiskAnalysisForm(
    version = "3.0",
    answers = Map(
      "purpose"                                         -> List("INSTITUTIONAL"),
      "institutionalPurpose"                            -> List("MyPurpose"),
      "personalDataTypes"                               -> List("OTHER"),
      "otherPersonalDataTypes"                          -> List("MyDataTypes"),
      "legalBasis"                                      -> List("LEGAL_OBLIGATION", "PUBLIC_INTEREST"),
      "legalObligationReference"                        -> List("somethingLegal"),
      "legalBasisPublicInterest"                        -> List("RULE_OF_LAW"),
      "ruleOfLawText"                                   -> List("TheLaw"),
      "knowsDataQuantity"                               -> List("NO"),
      "dataQuantity"                                    -> Nil,
      "deliveryMethod"                                  -> List("ANONYMOUS"),
      "policyProvided"                                  -> List("NO"),
      "confirmPricipleIntegrityAndDiscretion"           -> List("true"),
      "reasonPolicyNotProvided"                         -> List("Because"),
      "doneDpia"                                        -> List("NO"),
      "dataDownload"                                    -> List("YES"),
      "confirmDataRetentionPeriod"                      -> List("true"),
      "purposePursuit"                                  -> List("MERE_CORRECTNESS"),
      "checkedExistenceMereCorrectnessInteropCatalogue" -> List("true"),
      "usesThirdPartyData"                              -> List("NO"),
      "declarationConfirmGDPR"                          -> List("true")
    )
  )

  val validRiskAnalysis2_0_Private: RiskAnalysisForm = RiskAnalysisForm(
    version = "2.0",
    answers = Map(
      "purpose"                                         -> List("INSTITUTIONAL"),
      "institutionalPurpose"                            -> List("MyPurpose"),
      "usesPersonalData"                                -> List("YES"),
      "personalDataTypes"                               -> List("OTHER"),
      "otherPersonalDataTypes"                          -> List("MyDataTypes"),
      "legalBasis"                                      -> List("LEGAL_OBLIGATION", "PUBLIC_INTEREST"),
      "legalObligationReference"                        -> List("YES"),
      "legalBasisPublicInterest"                        -> List("RULE_OF_LAW"),
      "ruleOfLawText"                                   -> List("TheLaw"),
      "knowsDataQuantity"                               -> List("NO"),
      "dataQuantity"                                    -> Nil,
      "dataDownload"                                    -> List("YES"),
      "deliveryMethod"                                  -> List("CLEARTEXT"),
      "policyProvided"                                  -> List("NO"),
      "confirmPricipleIntegrityAndDiscretion"           -> List("true"),
      "reasonPolicyNotProvided"                         -> List("Because"),
      "doneDpia"                                        -> List("NO"),
      "dataRetentionPeriod"                             -> List("10"),
      "purposePursuit"                                  -> List("MERE_CORRECTNESS"),
      "checkedExistenceMereCorrectnessInteropCatalogue" -> List("true"),
      "declarationConfirmGDPR"                          -> List("true")
    )
  )

  val validOnlySchemaRiskAnalysis2_0: RiskAnalysisForm = RiskAnalysisForm(
    version = "3.0",
    answers = Map(
      "purpose"                    -> List("INSTITUTIONAL"),
      "usesPersonalData"           -> Nil,
      "usesThirdPartyPersonalData" -> Nil,
      "usesConfidentialData"       -> Nil
    )
  )

  val validOnlySchemaRiskAnalysis1_0: RiskAnalysisForm = RiskAnalysisForm(
    version = "2.0",
    answers = Map(
      "purpose"                    -> List("INSTITUTIONAL"),
      "usesPersonalData"           -> Nil,
      "usesThirdPartyPersonalData" -> Nil,
      "usesConfidentialData"       -> Nil
    )
  )
}
