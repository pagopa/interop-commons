package it.pagopa.interop.commons.riskanalysis

import cats.data.NonEmptyChain
import cats.syntax.all._
import cats.kernel.Eq
import it.pagopa.interop.commons.riskanalysis.api.impl.RiskAnalysisValidation
import it.pagopa.interop.commons.riskanalysis.api.impl.RiskAnalysisValidation.ValidationResult
import it.pagopa.interop.commons.riskanalysis.error._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.commons.riskanalysis.model._
import it.pagopa.interop.purposeprocess.SpecData

class RiskAnalysisValidationSpec extends AnyWordSpecLike {
  implicit val eqError: Eq[RiskAnalysisValidationError] = Eq.fromUniversalEquals

  "Risk Analysis Validation" should {
    "succeed on correct form 3.0 on tenant kind PA" in {

      val riskAnalysis: RiskAnalysisForm = SpecData.validRiskAnalysis3_0_Pa

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerValidated("legalObligationReference", Some("somethingLegal")),
          RiskAnalysisSingleAnswerValidated("dataDownload", Some("YES")),
          RiskAnalysisSingleAnswerValidated("checkedExistenceMereCorrectnessInteropCatalogue", Some("true")),
          RiskAnalysisSingleAnswerValidated("deliveryMethod", Some("ANONYMOUS")),
          RiskAnalysisSingleAnswerValidated("legalBasisPublicInterest", Some("RULE_OF_LAW")),
          RiskAnalysisSingleAnswerValidated("confirmPricipleIntegrityAndDiscretion", Some("true")),
          RiskAnalysisSingleAnswerValidated("usesThirdPartyData", Some("NO")),
          RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL")),
          RiskAnalysisSingleAnswerValidated("confirmDataRetentionPeriod", Some("true")),
          RiskAnalysisSingleAnswerValidated("ruleOfLawText", Some("TheLaw")),
          RiskAnalysisSingleAnswerValidated("otherPersonalDataTypes", Some("MyDataTypes")),
          RiskAnalysisSingleAnswerValidated("knowsDataQuantity", Some("NO")),
          RiskAnalysisSingleAnswerValidated("institutionalPurpose", Some("MyPurpose")),
          RiskAnalysisSingleAnswerValidated("policyProvided", Some("NO")),
          RiskAnalysisSingleAnswerValidated("reasonPolicyNotProvided", Some("Because")),
          RiskAnalysisSingleAnswerValidated("doneDpia", Some("NO")),
          RiskAnalysisSingleAnswerValidated("declarationConfirmGDPR", Some("true")),
          RiskAnalysisSingleAnswerValidated("purposePursuit", Some("MERE_CORRECTNESS"))
        ),
        multiAnswers = Seq(
          RiskAnalysisMultiAnswerValidated("personalDataTypes", Seq("OTHER")),
          RiskAnalysisMultiAnswerValidated("legalBasis", Seq("LEGAL_OBLIGATION", "PUBLIC_INTEREST"))
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PA)

      verifyValidationFormResult(result, expected)

    }

    "succeed on correct form 3.0 only schema on tenant kind PA" in {
      val riskAnalysis = SpecData.validOnlySchemaRiskAnalysis2_0

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL"))),
        multiAnswers = Seq.empty
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PA)

      verifyValidationFormResult(result, expected)

    }

    "succeed on correct form 2.0 on tenant kind PRIVATE" in {
      val riskAnalysis = SpecData.validRiskAnalysis2_0_Private

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerValidated("legalObligationReference", Some("YES")),
          RiskAnalysisSingleAnswerValidated("usesPersonalData", Some("YES")),
          RiskAnalysisSingleAnswerValidated("dataDownload", Some("YES")),
          RiskAnalysisSingleAnswerValidated("checkedExistenceMereCorrectnessInteropCatalogue", Some("true")),
          RiskAnalysisSingleAnswerValidated("deliveryMethod", Some("CLEARTEXT")),
          RiskAnalysisSingleAnswerValidated("legalBasisPublicInterest", Some("RULE_OF_LAW")),
          RiskAnalysisSingleAnswerValidated("confirmPricipleIntegrityAndDiscretion", Some("true")),
          RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL")),
          RiskAnalysisSingleAnswerValidated("ruleOfLawText", Some("TheLaw")),
          RiskAnalysisSingleAnswerValidated("dataRetentionPeriod", Some("10")),
          RiskAnalysisSingleAnswerValidated("otherPersonalDataTypes", Some("MyDataTypes")),
          RiskAnalysisSingleAnswerValidated("knowsDataQuantity", Some("NO")),
          RiskAnalysisSingleAnswerValidated("institutionalPurpose", Some("MyPurpose")),
          RiskAnalysisSingleAnswerValidated("policyProvided", Some("NO")),
          RiskAnalysisSingleAnswerValidated("reasonPolicyNotProvided", Some("Because")),
          RiskAnalysisSingleAnswerValidated("doneDpia", Some("NO")),
          RiskAnalysisSingleAnswerValidated("declarationConfirmGDPR", Some("true")),
          RiskAnalysisSingleAnswerValidated("purposePursuit", Some("MERE_CORRECTNESS"))
        ),
        multiAnswers = Seq(
          RiskAnalysisMultiAnswerValidated("personalDataTypes", Seq("OTHER")),
          RiskAnalysisMultiAnswerValidated("legalBasis", Seq("LEGAL_OBLIGATION", "PUBLIC_INTEREST"))
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.PRIVATE)

      verifyValidationFormResult(result, expected)

    }

    "succeed on correct form 2.0 only schema on tenant kind PRIVATE" in {
      val riskAnalysis = SpecData.validOnlySchemaRiskAnalysis1_0

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL"))),
        multiAnswers = Seq.empty
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PRIVATE)

      verifyValidationFormResult(result, expected)

    }

    "succeed on correct form 2.0 on tenant kind GSP" in {
      val riskAnalysis = SpecData.validRiskAnalysis2_0_Private

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerValidated("legalObligationReference", Some("YES")),
          RiskAnalysisSingleAnswerValidated("usesPersonalData", Some("YES")),
          RiskAnalysisSingleAnswerValidated("dataDownload", Some("YES")),
          RiskAnalysisSingleAnswerValidated("checkedExistenceMereCorrectnessInteropCatalogue", Some("true")),
          RiskAnalysisSingleAnswerValidated("deliveryMethod", Some("CLEARTEXT")),
          RiskAnalysisSingleAnswerValidated("legalBasisPublicInterest", Some("RULE_OF_LAW")),
          RiskAnalysisSingleAnswerValidated("confirmPricipleIntegrityAndDiscretion", Some("true")),
          RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL")),
          RiskAnalysisSingleAnswerValidated("ruleOfLawText", Some("TheLaw")),
          RiskAnalysisSingleAnswerValidated("dataRetentionPeriod", Some("10")),
          RiskAnalysisSingleAnswerValidated("otherPersonalDataTypes", Some("MyDataTypes")),
          RiskAnalysisSingleAnswerValidated("knowsDataQuantity", Some("NO")),
          RiskAnalysisSingleAnswerValidated("institutionalPurpose", Some("MyPurpose")),
          RiskAnalysisSingleAnswerValidated("policyProvided", Some("NO")),
          RiskAnalysisSingleAnswerValidated("reasonPolicyNotProvided", Some("Because")),
          RiskAnalysisSingleAnswerValidated("doneDpia", Some("NO")),
          RiskAnalysisSingleAnswerValidated("declarationConfirmGDPR", Some("true")),
          RiskAnalysisSingleAnswerValidated("purposePursuit", Some("MERE_CORRECTNESS"))
        ),
        multiAnswers = Seq(
          RiskAnalysisMultiAnswerValidated("personalDataTypes", Seq("OTHER")),
          RiskAnalysisMultiAnswerValidated("legalBasis", Seq("LEGAL_OBLIGATION", "PUBLIC_INTEREST"))
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.GSP)

      verifyValidationFormResult(result, expected)

    }

    "succeed on correct form 2.0 only schema on tenant kind GSP" in {
      val riskAnalysis = SpecData.validOnlySchemaRiskAnalysis1_0

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL"))),
        multiAnswers = Seq.empty
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.GSP)

      verifyValidationFormResult(result, expected)

    }

    "fail if version does not exists" in {
      val riskAnalysis = SpecData.validRiskAnalysis3_0_Pa.copy(version = "9999.0")

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.PA)

      verifyValidationFailure(result, _.contains(UnexpectedTemplateVersion("9999.0")) shouldBe true)

    }

    "fail if a provided answer depends on a missing field" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "3.0",
        answers = Map(
          "purpose"                    -> List("purpose"),
          "usesPersonalData"           -> List("YES"),
          "usesThirdPartyPersonalData" -> Nil,
          "usesConfidentialData"       -> List("YES")
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.PA)

      verifyValidationFailure(result, _.contains(MissingExpectedField("doneDpia")) shouldBe true)
      verifyValidationFailure(result, _.contains(MissingExpectedField("policyProvided")) shouldBe true)
      verifyValidationFailure(result, _.contains(MissingExpectedField("deliveryMethod")) shouldBe true)
    }

    "succeed only schema (fail if a provided answer depends on a missing field)" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "3.0",
        answers = Map(
          "purpose"                  -> List("INSTITUTIONAL"),
          "institutionalPurpose"     -> List("institutionalPurpose"),
          "otherPurpose"             -> List("otherPurpose"),
          "legalBasisPublicInterest" -> List("RULE_OF_LAW")
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PA)

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerValidated("purpose", Some("INSTITUTIONAL")),
          RiskAnalysisSingleAnswerValidated("institutionalPurpose", Some("institutionalPurpose")),
          RiskAnalysisSingleAnswerValidated("otherPurpose", Some("otherPurpose")),
          RiskAnalysisSingleAnswerValidated("legalBasisPublicInterest", Some("RULE_OF_LAW"))
        ),
        multiAnswers = Seq.empty
      )

      verifyValidationFormResult(result, expected)
    }

    "fail if a provided answer depends on an existing field with an unexpected value" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "3.0",
        answers = Map(
          "purpose"                  -> List("INSTITUTIONAL"),
          "institutionalPurpose"     -> List("institutionalPurpose"),
          "otherPurpose"             -> List("otherPurpose"),
          "legalBasisPublicInterest" -> List("RULE_OF_LAW")
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.PA)

      verifyValidationFailure(
        result,
        _.contains(UnexpectedFieldValueByDependency("purpose", "otherPurpose", "OTHER")) shouldBe true
      )
    }

    "succeed only schema (complete validation should fail because provided answer depends on an existing field with an unexpected value)" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "2.0",
        answers = Map(
          "purpose"                  -> List("INSTITUTIONAL"),
          "institutionalPurpose"     -> List("institutionalPurpose"),
          "otherPurpose"             -> List("otherPurpose"),
          "legalBasisPublicInterest" -> List("RULE_OF_LAW")
        )
      )

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(
          RiskAnalysisSingleAnswerValidated("purpose", "INSTITUTIONAL".some),
          RiskAnalysisSingleAnswerValidated("institutionalPurpose", "institutionalPurpose".some),
          RiskAnalysisSingleAnswerValidated("otherPurpose", "otherPurpose".some),
          RiskAnalysisSingleAnswerValidated("legalBasisPublicInterest", "RULE_OF_LAW".some)
        ),
        multiAnswers = Seq.empty
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PRIVATE)

      verifyValidationFormResult(result, expected)

    }

    "fail on missing expected answer (answer tree is not complete)" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "3.0",
        answers = Map(
          "purpose"                    -> List("purpose"),
          "usesPersonalData"           -> List("NO"),
          "usesThirdPartyPersonalData" -> List("YES"),
          "usesConfidentialData"       -> Nil,
          "securedDataAccess"          -> Nil
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, false)(RiskAnalysisTenantKind.PA)

      verifyValidationFailure(
        result,
        err =>
          (err.contains(UnexpectedField("usesPersonalData")) &&
            err.contains(UnexpectedFieldValue("purpose", Some(Set("INSTITUTIONAL", "OTHER")))) &&
            err.contains(UnexpectedField("usesThirdPartyPersonalData")) &&
            err.contains(MissingExpectedField("personalDataTypes")) &&
            err.contains(MissingExpectedField("legalBasis")) &&
            err.contains(MissingExpectedField("knowsDataQuantity")) &&
            err.contains(MissingExpectedField("deliveryMethod")) &&
            err.contains(MissingExpectedField("policyProvided")) &&
            err.contains(MissingExpectedField("confirmPricipleIntegrityAndDiscretion")) &&
            err.contains(MissingExpectedField("doneDpia")) &&
            err.contains(MissingExpectedField("dataDownload")) &&
            err.contains(MissingExpectedField("purposePursuit")) &&
            err.contains(MissingExpectedField("usesThirdPartyData")) &&
            err.contains(MissingExpectedField("declarationConfirmGDPR"))) shouldBe true
      )
    }

    "succeed only schema (complete validation should fail because missing expected answer, as tree is not complete)" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "3.0",
        answers = Map("purpose" -> List("INSTITUTIONAL"), "usesConfidentialData" -> Nil, "securedDataAccess" -> Nil)
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PA)

      val expected = RiskAnalysisFormSeed(
        version = riskAnalysis.version,
        singleAnswers = Seq(RiskAnalysisSingleAnswerValidated("purpose", "INSTITUTIONAL".some)),
        multiAnswers = Seq.empty
      )

      verifyValidationFormResult(result, expected)

    }

    "fail on unexpected field name in only schema validation" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "2.0",
        answers = Map(
          "purpose1"                   -> List("purpose"),
          "usesPersonalData"           -> List("NO"),
          "usesThirdPartyPersonalData" -> List("YES"),
          "usesConfidentialData"       -> Nil,
          "securedDataAccess"          -> Nil
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PRIVATE)

      verifyValidationFailure(result, _.contains(UnexpectedField("purpose1")) shouldBe true)

    }

    "fail on unexpected field value in only schema validation" in {
      val riskAnalysis = RiskAnalysisForm(
        version = "2.0",
        answers = Map(
          "purpose"                    -> List("purpose"),
          "usesPersonalData"           -> List("pippo"),
          "usesThirdPartyPersonalData" -> List("YES"),
          "usesConfidentialData"       -> Nil,
          "securedDataAccess"          -> Nil
        )
      )

      val result: ValidationResult[RiskAnalysisFormSeed] =
        RiskAnalysisValidation.validate(riskAnalysis, true)(RiskAnalysisTenantKind.PRIVATE)

      verifyValidationFailure(
        result,
        _.contains(UnexpectedFieldValue("usesPersonalData", Option(Set("YES", "NO")))) shouldBe true
      )
      verifyValidationFailure(
        result,
        _.contains(UnexpectedFieldValue("purpose", Option(Set("INSTITUTIONAL", "OTHER")))) shouldBe true
      )
    }
  }

  def verifyValidationFormResult(
    result: ValidationResult[RiskAnalysisFormSeed],
    expected: RiskAnalysisFormSeed
  ): Assertion = {
    result.fold(
      err => fail(s"Unexpected validation failure: ${err.toString}"),
      r => {
        r.version shouldBe expected.version
        r.singleAnswers should contain theSameElementsAs expected.singleAnswers
        r.multiAnswers should contain theSameElementsAs expected.multiAnswers
      }
    )
  }

  def verifyValidationFailure(
    result: ValidationResult[RiskAnalysisFormSeed],
    errorAssertion: NonEmptyChain[RiskAnalysisValidationError] => Assertion
  ): Assertion =
    result.fold(errorAssertion, result => fail(s"Unexpected validation success $result"))

}
