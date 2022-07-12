package it.pagopa.interop.commons

import buildinfo.BuildInfo
import cats.syntax.all._
import akka.event.Logging
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.CanLog
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, IP_ADDRESS, SUB, UID}

import java.util.UUID

package object logging {
  type ContextFieldsToLog = Seq[(String, String)]

  private val config: Config            = ConfigFactory.load()
  private val isInternetFacing: Boolean =
    if (config.hasPath("interop-commons.isInternetFacing")) config.getBoolean("interop-commons.isInternetFacing")
    else false

  private def createLogContexts(values: Map[String, String]): String = {
    val ipAddress: String     = values.getOrElse(IP_ADDRESS, "")
    val uid: String           = values.get(UID).filterNot(_.isBlank).orElse(values.get(SUB)).getOrElse("")
    val correlationId: String = values.getOrElse(CORRELATION_ID_HEADER, "")
    s"[IP=$ipAddress] [UID=$uid] [CID=$correlationId]"
  }

  /** Defines log message decoration for Interop
    */
  implicit case object CanLogContextFields extends CanLog[ContextFieldsToLog] {
    override def logMessage(originalMsg: String, fields: ContextFieldsToLog): String = {
      val fieldsMap: Map[String, String] = fields.toMap
      s"${createLogContexts(fieldsMap)} - $originalMsg"
    }
  }

  /** Returns a directive to decorate HTTP requests with logging
    *
    * @param ctxs contexts to be logged
    * @return logging directive
    */
  def logHttp(enabled: Boolean)(implicit ctxs: Seq[(String, String)]): Directive0 = if (enabled) {
    val contextStr: String = createLogContexts(ctxs.toMap)

    def logWithoutBody(req: HttpRequest): RouteResult => Option[LogEntry] = {
      case RouteResult.Complete(res) =>
        Some(
          LogEntry(s"$contextStr - Request ${req.method.value} ${req.uri} - Response ${res.status}", Logging.InfoLevel)
        )
      case RouteResult.Rejected(rej) =>
        Some(LogEntry(s"$contextStr - Request ${req.method.value} ${req.uri} - Response ${rej}", Logging.InfoLevel))
    }

    DebuggingDirectives.logRequestResult(logWithoutBody _)
  } else Directive.Empty

  /** Generates a wrapping directive with logging context attributes with given configurations
    *
    * @param wrappingDirective directive to be decorated
    * @return directive with contexts enriched with logging attributes
    */
  def withLoggingAttributesGenerator(
    isInternetFacing: Boolean
  ): Directive1[Seq[(String, String)]] => Directive1[Seq[(String, String)]] = { wrappingDirective =>
    extractClientIP.flatMap(ip => {
      val ipAddress = ip.toOption.map(_.getHostAddress).getOrElse("unknown")

      optionalHeaderValueByName(CORRELATION_ID_HEADER).flatMap(correlationId => {
        // Exclude headers for security reason if the service is internet facing
        val actualCorrelationId =
          if (isInternetFacing) UUID.randomUUID().toString else correlationId.getOrElse(UUID.randomUUID().toString)
        wrappingDirective.map(contexts =>
          contexts
            .prepended((CORRELATION_ID_HEADER, actualCorrelationId))
            .prepended((IP_ADDRESS, ipAddress))
        )
      })
    })
  }

  /** Enriches a wrapping directive with logging context attributes
    *
    * @param wrappingDirective directive to be decorated
    * @return directive with contexts enriched with logging attributes
    */
  val withLoggingAttributes: Directive1[Seq[(String, String)]] => Directive1[Seq[(String, String)]] =
    (wrappingDirective: Directive1[Seq[(String, String)]]) =>
      withLoggingAttributesGenerator(isInternetFacing)(wrappingDirective)

  def renderBuildInfo(buildInfo: BuildInfo.type): String = buildInfo.toMap
    .collect {
      case (k, v: Option[_]) if v.isDefined => s"$k: ${v.get}"
      case (k, v)                           => s"$k: $v"
    }
    .mkString("[Build Info] ", ", ", "")

}
