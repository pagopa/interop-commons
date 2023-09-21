package it.pagopa.interop.commons.riskanalysis.api.impl

import cats.data._
import cats.syntax.all._

import it.pagopa.interop.commons.riskanalysis.error._
import it.pagopa.interop.commons.riskanalysis.model._
import it.pagopa.interop.commons.riskanalysis.model.riskAnalysisRules.{DependencyEntry, ValidationEntry}
import it.pagopa.interop.commons.riskanalysis.model.riskAnalysisTemplate._
import it.pagopa.interop.commons.riskanalysis.service.RiskAnalysisService
import spray.json._

object RiskAnalysisValidation {

  type ValidationResult[A] = ValidatedNec[RiskAnalysisValidationError, A]

  /** Validate a Process risk analysis form and returns the same in the Management format
    * @param form Risk Analysis Form
    * @param kind The Tenant Kind
    * @return Validated risk analysis
    */
  def validate(form: RiskAnalysisForm, schemaOnlyValidation: Boolean)(
    kind: RiskAnalysisTenantKind
  ): ValidationResult[RiskAnalysisFormSeed] = {
    RiskAnalysisService
      .riskAnalysisForms()
      .get(kind)
      .fold[ValidationResult[RiskAnalysisFormSeed]](MissingTenantKindConfiguration(kind).invalidNec)(
        validateLatestVersion(_, kind, schemaOnlyValidation)(form)
      )
  }

  /** Validate a Process risk analysis form and returns the same in the Management format
    * @param versions Versions for this Tenant Kind
    * @param tenantkind Tenant Kind
    * @param form Risk Analysis Form
    * @param schemaOnlyValidation flag indicating if should validate only schema
    * @return Validated risk analysis
    */
  private def validateLatestVersion(
    versions: Map[String, RiskAnalysisFormConfig],
    tenantkind: RiskAnalysisTenantKind,
    schemaOnlyValidation: Boolean
  )(form: RiskAnalysisForm): ValidationResult[RiskAnalysisFormSeed] = {
    val sanitizedForm = form.copy(answers = form.answers.filter(_._2.nonEmpty))

    val validationRules: ValidationResult[List[ValidationEntry]] =
      versions
        .maxByOption(_._1.toDouble)
        .fold[ValidationResult[List[ValidationEntry]]](NoTemplateVersionFound(tenantkind).invalidNec)(v =>
          if (v._1 == form.version) configsToRules(v._2).validNec
          else UnexpectedTemplateVersion(form.version).invalidNec
        )

    validationRules.andThen(validateFormWithRules(_, sanitizedForm, schemaOnlyValidation))
  }

  private def validateFormWithRules(
    validationRules: List[ValidationEntry],
    form: RiskAnalysisForm,
    schemaOnlyValidation: Boolean
  ): ValidationResult[RiskAnalysisFormSeed] = {
    val answersJson: JsObject = form.answers.toJson.asJsObject
    val validations           = validateForm(answersJson, validationRules, schemaOnlyValidation)

    val singleAnswers: ValidationResult[Seq[RiskAnalysisSingleAnswerValidated]] = validations.collect { case Left(s) =>
      s
    }.sequence
    val multiAnswers: ValidationResult[Seq[RiskAnalysisMultiAnswerValidated]]   = validations.collect { case Right(m) =>
      m
    }.sequence
    val expectedFields: ValidationResult[List[Unit]]                            =
      if (schemaOnlyValidation) List(()).validNec else validateExpectedFields(answersJson, validationRules)

    (singleAnswers, multiAnswers, expectedFields).mapN((l1, l2, _) =>
      RiskAnalysisFormSeed(version = form.version, singleAnswers = l1, multiAnswers = l2)
    )
  }

  /** Verify that each field is present when the related dependencies are met
    * @param answersJson form in json format
    * @param validationRules validation rules
    * @return
    */
  private def validateExpectedFields(
    answersJson: JsObject,
    validationRules: List[ValidationEntry]
  ): ValidationResult[List[Unit]] = {
    validationRules
      .filter(_.required)
      .map { entry =>
        val dependenciesSatisfied: Boolean = entry.dependencies.forall(formContainsDependency(answersJson, _))

        if (
          !dependenciesSatisfied ||
          (dependenciesSatisfied && answersJson.getFields(entry.fieldName).nonEmpty)
        )
          ().validNec
        else
          MissingExpectedField(entry.fieldName).invalidNec
      }
      .sequence
  }

  /** Check if the form contains the dependency (field with the specific value)
    * @param answersJson form in json format
    * @param dependency dependency
    * @return true if contained, false otherwise
    */
  private def formContainsDependency(answersJson: JsObject, dependency: DependencyEntry): Boolean =
    answersJson.getFields(dependency.fieldName).exists { f =>
      f match {
        case str: JsString => str.value == dependency.fieldValue
        case arr: JsArray  => jsArrayContains(arr, dependency.fieldValue)
        case _             => false
      }
    }

  /** Validate the form using the validation rules
    * @param answersJson form in json format
    * @param validationRules validation rules
    * @param schemaOnlyValidation flag indicating if should validate only schema
    * @return List of validations for single and multiple answers
    */
  private def validateForm(
    answersJson: JsObject,
    validationRules: List[ValidationEntry],
    schemaOnlyValidation: Boolean
  ): Seq[
    Either[ValidationResult[RiskAnalysisSingleAnswerValidated], ValidationResult[RiskAnalysisMultiAnswerValidated]]
  ] = {
    answersJson.fields.map { case (key, value) =>
      validateField(answersJson, validationRules, schemaOnlyValidation)(key, value).fold(
        err => Left[ValidationResult[RiskAnalysisSingleAnswerValidated], Nothing](err.invalid),
        rule => answerToDependency(rule, key, value)
      )
    }.toSeq
  }

  /** Convert a form answer to a Management answer
    * @param rule Validation Entry
    * @param fieldName form field name
    * @param value form field value
    * @return Either for the validation of the SingleAnswer (Left) or MultiAnswer (Right)
    */
  private def answerToDependency(
    rule: ValidationEntry,
    fieldName: String,
    value: JsValue
  ): Either[ValidationResult[RiskAnalysisSingleAnswerValidated], ValidationResult[RiskAnalysisMultiAnswerValidated]] =
    (value, rule.dataType) match {
      case (arr: JsArray, Single | FreeText) =>
        val value: ValidationResult[String] = {
          arr.elements.headOption match {
            case Some(str: JsString) => str.value.validNec
            case _                   => UnexpectedFieldFormat(fieldName).invalidNec
          }
        }
        Left(value.map(v => RiskAnalysisSingleAnswerValidated(fieldName, v.some)))
      case (arr: JsArray, Multi)             =>
        val values: Seq[ValidationResult[String]] = {
          arr.elements.map {
            case str: JsString => str.value.validNec
            case _             => UnexpectedFieldFormat(fieldName).invalidNec
          }
        }
        Right(values.sequence.map(RiskAnalysisMultiAnswerValidated(fieldName, _)))
      case _                                 =>
        Left(UnexpectedFieldFormat(fieldName).invalidNec)
    }

  /** Verify that the field found in the form satisfies the validation rules
    * @param answersJson form in json format
    * @param validationRules validation rules
    * @param fieldName field name
    * @return
    */
  private def validateField(
    answersJson: JsObject,
    validationRules: List[ValidationEntry],
    schemaOnlyValidation: Boolean
  )(fieldName: String, value: JsValue): ValidationResult[ValidationEntry] =
    validationRules.find(_.fieldName == fieldName) match {
      case Some(rule) if (schemaOnlyValidation) =>
        validateAllowedValue(rule, value)
          .as(rule)
      case Some(rule)                           =>
        validateAllowedValue(rule, value)
          .andThen(_ => rule.dependencies.traverse(validateDependency(answersJson, rule.fieldName)))
          .as(rule)
      case None                                 =>
        UnexpectedField(fieldName).invalidNec
    }

  /** Verify that the form contains the dependency
    * @param answersJson form in json format
    * @param dependency dependency
    * @return Validation result
    */
  private def validateDependency(answersJson: JsObject, dependentField: String)(
    dependency: DependencyEntry
  ): ValidationResult[Unit] =
    answersJson.getFields(dependency.fieldName) match {
      case Nil      =>
        DependencyNotFound(dependency.fieldName, dependentField).invalidNec
      case f :: Nil =>
        f match {
          case str: JsString if str.value == dependency.fieldValue         =>
            ().validNec
          case arr: JsArray if jsArrayContains(arr, dependency.fieldValue) =>
            ().validNec
          case _                                                           =>
            UnexpectedFieldValueByDependency(dependency.fieldName, dependentField, dependency.fieldValue).invalidNec
        }
      case _        => TooManyOccurrences(dependency.fieldName).invalidNec
    }

  private def validateAllowedValue(rule: ValidationEntry, value: JsValue): ValidationResult[Unit] =
    value match {
      case str: JsString =>
        if (rule.allowedValues.forall(_.contains(str.value))) ().validNec
        else UnexpectedFieldValue(rule.fieldName, rule.allowedValues).invalidNec
      case arr: JsArray  =>
        rule.allowedValues.fold[ValidationResult[Unit]](().validNec)(jsArrayIsSubset(rule.fieldName, arr, _))
      case _             =>
        UnexpectedFieldValue(rule.fieldName, rule.allowedValues).invalidNec
    }

  /** Check if a JsArray contains a specific String (JsString)
    * @param arr JsArray
    * @param value the string
    * @return true if array contains the string, false otherwise
    */
  private def jsArrayContains(arr: JsArray, value: String): Boolean =
    arr.elements.collectFirst { case v: JsString if v.value == value => () }.nonEmpty

  private def jsArrayIsSubset(fieldName: String, arr: JsArray, values: Set[String]): ValidationResult[Unit] =
    arr.elements.traverse {
      case v: JsString if values.contains(v.value) => ().validNec
      case _                                       => UnexpectedFieldValue(fieldName, values.some).invalidNec
    }.void

  private def dependencyConfigToRule(dependency: Dependency): DependencyEntry =
    DependencyEntry(fieldName = dependency.id, fieldValue = dependency.value)

  private def questionToValidationEntry(question: FormConfigQuestion): ValidationEntry =
    question match {
      case c: FreeInputQuestion =>
        ValidationEntry(
          fieldName = c.id,
          dataType = c.dataType,
          required = c.required,
          dependencies = c.dependencies.map(dependencyConfigToRule),
          allowedValues = None
        )
      case c: SingleQuestion    =>
        ValidationEntry(
          fieldName = c.id,
          dataType = c.dataType,
          required = c.required,
          dependencies = c.dependencies.map(dependencyConfigToRule),
          allowedValues = c.options.map(_.value).toSet.some
        )
      case c: MultiQuestion     =>
        ValidationEntry(
          fieldName = c.id,
          dataType = c.dataType,
          required = c.required,
          dependencies = c.dependencies.map(dependencyConfigToRule),
          allowedValues = c.options.map(_.value).toSet.some
        )
    }

  private def configsToRules(config: RiskAnalysisFormConfig): List[ValidationEntry] =
    config.questions.map(questionToValidationEntry)

}
