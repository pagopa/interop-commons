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

  private def completeWithError(statusCode: StatusCode, headers: List[HttpHeader], error: ComponentError)(implicit
    contexts: Seq[(String, String)],
    serviceCode: ServiceCode
  ): StandardRoute =
    complete(statusCode.intValue, headers, Problem(statusCode, error, serviceCode, getCorrelationId(contexts)))

  private def completeWithErrors(statusCode: StatusCode, headers: List[HttpHeader], errors: List[ComponentError])(
    implicit
    contexts: Seq[(String, String)],
    serviceCode: ServiceCode
  ): StandardRoute =
    complete(statusCode.intValue, headers, Problem(statusCode, errors, serviceCode, getCorrelationId(contexts)))

  @inline private def getCorrelationId(contexts: Seq[(String, String)]): Option[String] =
    contexts.collectFirst { case (k, v) if k == CORRELATION_ID_HEADER => v }

  def badRequest(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    badRequest(error, logMessage, Nil)
  }

  def badRequest(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.BadRequest, headers, error)
  }

  def badRequest(errors: List[ComponentError], logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    badRequest(errors, logMessage, Nil)
  }

  def badRequest(errors: List[ComponentError], logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(s"$logMessage. Reasons: ${errors.mapFilter(e => Option(e.getMessage)).mkString("[", ",", "]")}")
    completeWithErrors(StatusCodes.BadRequest, headers, errors)
  }

  def unauthorized(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    unauthorized(error, logMessage, Nil)
  }

  def unauthorized(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Unauthorized, headers, error)
  }

  def notFound(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    notFound(error, logMessage, Nil)
  }

  def notFound(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.NotFound, headers, error)
  }

  def forbidden(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    forbidden(error, logMessage, Nil)
  }

  def forbidden(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Forbidden, headers, error)
  }

  def conflict(error: ComponentError, logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    conflict(error, logMessage, Nil)
  }

  def conflict(error: ComponentError, logMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.warn(logMessage, error)
    completeWithError(StatusCodes.Conflict, headers, error)
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
    internalServerError(error, errorMessage, Nil)
  }

  def internalServerError(error: Throwable, errorMessage: String, headers: List[HttpHeader])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): StandardRoute = {
    logger.error(errorMessage, error)
    completeWithError(StatusCodes.InternalServerError, headers, GenericError(errorMessage))
  }
}

object AkkaResponses extends AkkaResponses
