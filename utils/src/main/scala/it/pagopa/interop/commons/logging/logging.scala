package it.pagopa.interop.commons

import akka.event.Logging
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import com.typesafe.scalalogging.CanLog
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, UID, IP_ADDRESS, SUB}

package object logging {
  type ContextFieldsToLog = Seq[(String, String)]

  @inline private def optToLog(opt: Option[String]): String = opt.getOrElse("")

  private def contexts(values: Map[String, String]): String = {
    val ipAddress     = optToLog(values.get(IP_ADDRESS))
    val subject       = optToLog(values.get(UID).filterNot(_.isBlank).orElse(values.get(SUB)))
    val correlationId = optToLog(values.get(CORRELATION_ID_HEADER))
    s"[$ipAddress] [$subject] [$correlationId]"
  }

  /** Defines log message decoration for Interop
    */
  implicit case object CanLogContextFields extends CanLog[ContextFieldsToLog] {
    override def logMessage(originalMsg: String, fields: ContextFieldsToLog): String = {
      val fieldsMap = fields.toMap
      s"[${optToLog(fieldsMap.get(UID))}] [${optToLog(fieldsMap.get(CORRELATION_ID_HEADER))}] - $originalMsg"
    }
  }

  /** Returns a directive to decorate HTTP requests with logging
    *
    * @param ctxs contexts to be logged
    * @return logging directive
    */
  def logHttp(ctxs: Seq[(String, String)]): Directive0 = {
    val contextStr = contexts(ctxs.toMap)

    def logWithoutBody(req: HttpRequest): RouteResult => Option[LogEntry] = {
      case RouteResult.Complete(res) =>
        Some(LogEntry(s"$contextStr - Request ${req.uri} - Response ${res.status}", Logging.InfoLevel))
      case RouteResult.Rejected(rej) =>
        Some(LogEntry(s"$contextStr - Request ${req.uri} - Response ${rej}", Logging.InfoLevel))
    }

    DebuggingDirectives.logRequestResult(logWithoutBody _)
  }

  /** Enriches a wrapping directive with logging context attributes
    *
    * @param wrappingDirective directive to be decorated
    * @return directive with contexts enriched with logging attributes
    */
  def withLoggingAttributes(wrappingDirective: Directive1[Seq[(String, String)]]): Directive1[Seq[(String, String)]] = {
    extractClientIP.flatMap(ip => {
      val ipAddress = ip.toOption.map(_.getHostAddress).getOrElse("unknown")

      optionalHeaderValueByName(CORRELATION_ID_HEADER).flatMap(correlationId => {
        wrappingDirective.map(contexts =>
          contexts
            .prepended((CORRELATION_ID_HEADER, correlationId.getOrElse("")))
            .prepended((IP_ADDRESS, ipAddress))
        )
      })
    })
  }
}
