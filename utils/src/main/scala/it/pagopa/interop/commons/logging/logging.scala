package it.pagopa.interop.commons

import buildinfo.BuildInfo
import akka.http.scaladsl.server.Directives.{selectPreferredLanguage, optionalHeaderValueByName, _}
import akka.http.scaladsl.server._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.CanLog
import it.pagopa.interop.commons.utils.{
  CORRELATION_ID_HEADER,
  IP_ADDRESS,
  ORGANIZATION_ID_CLAIM,
  SUB,
  UID,
  ACCEPT_LANGUAGE,
  DEFAULT_LANGUAGE,
  OTHER_LANGUAGES
}

import java.util.UUID
import org.slf4j.MDC
import com.typesafe.scalalogging.LoggerTakingImplicit
import akka.http.scaladsl.model.HttpRequest

package object logging {
  type ContextFieldsToLog = Seq[(String, String)]

  @inline private def contextOrBlank(cftl: ContextFieldsToLog, key: String): String =
    cftl.find(_._1 == key).map(_._2).filterNot(_.isBlank()).getOrElse("")

  implicit case object CanLogContextFields extends CanLog[ContextFieldsToLog] {
    override def logMessage(originalMsg: String, fields: ContextFieldsToLog): String = {
      MDC.put(IP_ADDRESS, contextOrBlank(fields, IP_ADDRESS))
      MDC.put(UID, contextOrBlank(fields, UID))
      MDC.put(SUB, contextOrBlank(fields, SUB))
      MDC.put(ORGANIZATION_ID_CLAIM, contextOrBlank(fields, ORGANIZATION_ID_CLAIM))
      MDC.put(CORRELATION_ID_HEADER, contextOrBlank(fields, CORRELATION_ID_HEADER))

      originalMsg
    }

    override def afterLog(context: ContextFieldsToLog): Unit = {
      MDC.remove(IP_ADDRESS)
      MDC.remove(UID)
      MDC.remove(SUB)
      MDC.remove(ORGANIZATION_ID_CLAIM)
      MDC.remove(CORRELATION_ID_HEADER)
    }
  }

  private val config: Config            = ConfigFactory.load()
  private val isInternetFacing: Boolean =
    if (config.hasPath("interop-commons.isInternetFacing")) config.getBoolean("interop-commons.isInternetFacing")
    else false

  def withLoggingAttributes: Directive1[Seq[(String, String)]] => Directive1[Seq[(String, String)]] =
    withLoggingAttributesF(isInternetFacing)(_)

  def withLoggingAttributesF(
    changeUUID: Boolean
  )(wrappingDirective: Directive1[Seq[(String, String)]]): Directive1[Seq[(String, String)]] =
    for {
      ip            <- extractClientIP
      correlationId <- optionalHeaderValueByName(CORRELATION_ID_HEADER)
      language      <- selectPreferredLanguage(DEFAULT_LANGUAGE, OTHER_LANGUAGES: _*)
      contexts      <- wrappingDirective
    } yield {
      val ipAddress: String           = ip.toOption.map(_.getHostAddress).getOrElse("unknown")
      def uuid: String                = UUID.randomUUID().toString
      val actualCorrelationId: String = if (changeUUID) uuid else correlationId.getOrElse(uuid)
      val acceptLanguage: String      = language.toString

      contexts.prependedAll(
        List(CORRELATION_ID_HEADER -> actualCorrelationId, IP_ADDRESS -> ipAddress, ACCEPT_LANGUAGE -> acceptLanguage)
      )
    }

  def logHttp(
    enabled: Boolean
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog], contexts: ContextFieldsToLog): Directive0 =
    if (!enabled) Directive.Empty
    else
      extractRequest.flatMap { req: HttpRequest =>
        val header: String = s"Request ${req.method.value} ${req.uri} - Response"
        mapRouteResult {
          case x @ RouteResult.Complete(res) => logger.info(s"$header ${res.status}"); x
          case x @ RouteResult.Rejected(rej) => logger.info(s"$header ${rej}"); x
        }
      }

  def renderBuildInfo(buildInfo: BuildInfo.type): String = buildInfo.toMap
    .collect {
      case (k, v: Option[_]) if v.isDefined => s"$k: ${v.get}"
      case (k, v)                           => s"$k: $v"
    }
    .mkString("[Build Info] ", ", ", "")

}
