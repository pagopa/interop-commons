package it.pagopa.interop.commons.utils.errors

import akka.http.scaladsl.model.{HttpHeader, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import cats.implicits.toFunctorFilterOps
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError

trait AkkaResponses {

  private def completeWithError(statusCode: StatusCode, error: ComponentError)(implicit
    contexts: Seq[(String, String)],
    serviceCode: ServiceCode
  ): StandardRoute = complete(statusCode.intValue, Problem(statusCode, error, serviceCode, getCorrelationId(contexts)))

  private def completeWithErrors(statusCode: StatusCode, errors: List[ComponentError])(implicit
    contexts: Seq[(String, String)],
    serviceCode: ServiceCode
  ): StandardRoute = complete(statusCode.intValue, Problem(statusCode, errors, serviceCode, getCorrelationId(contexts)))

  private def completeWithError(statusCode: StatusCode, headers: List[HttpHeader], error: ComponentError)(implicit
    contexts: Seq[(String, String)],
    serviceCode: ServiceCode
  ): StandardRoute =
    complete(statusCode.intValue, headers, Problem(statusCode, error, serviceCode, getCorrelationId(contexts)))

  private def getCorrelationId(contexts: Seq[(String, String)]): Option[String] =
    contexts.collectFirst { case (k, v) if k == CORRELATION_ID_HEADER => v }

  def badRequest(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.BadRequest, error)
  }

  def badRequest(errors: List[ComponentError], logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(s"$logMessage. Reasons: ${errors.mapFilter(e => Option(e.getMessage)).mkString("[", ",", "]")}")
    completeWithErrors(StatusCodes.BadRequest, errors)
  }

  def unauthorized(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Unauthorized, error)
  }

  def notFound(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.NotFound, error)
  }

  def forbidden(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Forbidden, error)
  }

  def conflict(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Conflict, error)
  }

  def tooManyRequests(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.TooManyRequests, headers, error)
  }

  def internalServerError(error: Throwable, errorMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.error(errorMessage, error)
    completeWithError(StatusCodes.InternalServerError, GenericError(errorMessage))
  }
}

object AkkaResponses extends AkkaResponses
