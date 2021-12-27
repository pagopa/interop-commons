package it.pagopa.pdnd.interop.commons.utils

import com.atlassian.oai.validator.report.ValidationReport
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.StringOps
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

trait OpenapiUtils {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def parseArrayParameters(params: String): List[String] = {
    if (params == "[]") List.empty else params.parseCommaSeparated
  }

  def verifyParametersByCondition[A](params: List[A]): A => Boolean = { s =>
    params.isEmpty || params.contains(s)
  }

  def errorFromRequestValidationReport(report: ValidationReport): String = {
    val messageStrings = report.getMessages.asScala.foldLeft[List[String]](List.empty)((tail, m) => {
      val context = m.getContext.toScala.map(c =>
        Seq(c.getRequestMethod.toScala, c.getRequestPath.toScala, c.getLocation.toScala).flatten
      )
      s"""${m.getAdditionalInfo.asScala.mkString(",")}
         |${m.getLevel} - ${m.getMessage}
         |${context.getOrElse(Seq.empty).mkString(" - ")}
         |""".stripMargin :: tail
    })

    logger.error("Request failed: {}", messageStrings.mkString)
    report.getMessages().asScala.map(_.getMessage).mkString(", ")
  }
}

object OpenapiUtils extends OpenapiUtils
