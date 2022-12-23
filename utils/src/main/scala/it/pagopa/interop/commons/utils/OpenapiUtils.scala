package it.pagopa.interop.commons.utils

import com.atlassian.oai.validator.report.ValidationReport
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

object OpenapiUtils {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def parseArrayParameters(params: String): List[String] =
    if (params == "[]") List.empty else params.parseCommaSeparated

  def verifyParametersByCondition[A](params: List[A]): A => Boolean = { s => params.isEmpty || params.contains(s) }

  def errorFromRequestValidationReport(report: ValidationReport): List[ValidationRequestError] = {
    val errors: List[ValidationRequestError] = report.getMessages.asScala.toList.map { m =>
      m.getContext.toScala
        .flatMap(_.getParameter.toScala)
        .map(param => ValidationRequestError(s"${param.getName} is not valid - ${m.getMessage}"))
        .getOrElse(ValidationRequestError(s"Invalid parameter found - ${m.getMessage}"))
    }

    val messageStrings: String = report.getMessages.asScala.headOption
      .map(m =>
        m.getContext.toScala
          .map(c => Seq(c.getRequestMethod.toScala.map(_.name()), c.getRequestPath.toScala).flatten)
          .getOrElse(Seq.empty)
          .mkString(" - ")
      )
      .getOrElse("No request information")

    logger.warn(s"""Request failed: $messageStrings - ["${errors.map(_.msg).mkString("""", """")}"]"""")

    errors

  }
}
