package it.pagopa.interop.commons.utils.errors

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError

trait AkkaResponses {

  val serviceErrorCodePrefix: String

  private def completeWithError(statusCode: StatusCode, error: ComponentError): StandardRoute =
    complete(statusCode.intValue, Problem(statusCode, error, serviceErrorCodePrefix))

  def badRequest(error: ComponentError, errorMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): StandardRoute = {
    logger.warn(errorMessage, error)
    completeWithError(StatusCodes.BadRequest, error)
  }

  def notFound(error: ComponentError, errorMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): StandardRoute = {
    logger.warn(errorMessage, error)
    completeWithError(StatusCodes.NotFound, error)
  }

  def forbidden(error: ComponentError, errorMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): StandardRoute = {
    logger.warn(errorMessage, error)
    completeWithError(StatusCodes.Forbidden, error)
  }

  def internalServerError(error: Throwable, errorMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): StandardRoute = {
    logger.error(errorMessage, error)
    val statusCode = StatusCodes.InternalServerError
    complete(statusCode.intValue, Problem(statusCode, GenericError(errorMessage), serviceErrorCodePrefix))
  }
}
