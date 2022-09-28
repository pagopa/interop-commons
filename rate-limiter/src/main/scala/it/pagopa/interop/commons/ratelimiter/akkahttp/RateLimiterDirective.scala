package it.pagopa.interop.commons.ratelimiter.akkahttp

import cats.syntax.all._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{mapResponseHeaders, onComplete, provide}
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.ratelimiter.RateLimiter
import it.pagopa.interop.commons.ratelimiter.akkahttp.Errors.MissingOrganizationIdClaim
import it.pagopa.interop.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.interop.commons.ratelimiter.model.Headers.{
  RATE_LIMITER_HEADER_INTERVAL,
  RATE_LIMITER_HEADER_LIMIT,
  RATE_LIMITER_HEADER_REMAINING,
  headersFromContext
}
import it.pagopa.interop.commons.ratelimiter.model.{Headers, RateLimitStatus}
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object RateLimiterDirective {

  def rateLimiterDirective[T](rateLimiter: RateLimiter, tooManyRequestsProblem: T)(
    contexts: Seq[(String, String)]
  )(implicit
    ec: ExecutionContext,
    toEntityMarshallerProblem: ToEntityMarshaller[T],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Directive1[Seq[(String, String)]] =
    rateLimit(rateLimiter, tooManyRequestsProblem)(contexts).flatMap(addHeaders)

  private def rateLimit[T](rateLimiter: RateLimiter, tooManyRequestsProblem: T)(
    contexts: Seq[(String, String)]
  )(implicit
    ec: ExecutionContext,
    toEntityMarshallerProblem: ToEntityMarshaller[T],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Directive1[Seq[(String, String)]] =
    getOrganizationIdUUID(contexts) match {
      case Right(orgId) =>
        implicit val c: Seq[(String, String)] = contexts
        onComplete(rateLimiter.rateLimiting(orgId)).flatMap {
          case Success(status)               => provide(contexts ++ statusToContext(status))
          case Failure(tmr: TooManyRequests) =>
            complete(StatusCodes.TooManyRequests, Headers.headersFromStatus(tmr.status), tooManyRequestsProblem)
          // Never interrupt execution in case of unexpected rate limiting failure
          case Failure(err)                  =>
            logger.error(s"Unexpected error during rate limiting for organization $orgId", err)
            provide(contexts)
        }

      case Left(_) =>
        logger.error(s"Missing or not correctly formatted $ORGANIZATION_ID_CLAIM")(contexts)
        reject(MissingOrganizationIdClaim)
    }

  private def addHeaders(contexts: Seq[(String, String)]): Directive1[Seq[(String, String)]] =
    mapResponseHeaders(_ ++ headersFromContext(contexts)).tmap(_ => contexts)

  private def statusToContext(status: RateLimitStatus): Seq[(String, String)] =
    Seq(
      (RATE_LIMITER_HEADER_LIMIT, status.maxRequests.toString),
      (RATE_LIMITER_HEADER_INTERVAL, status.rateInterval.toMillis.toString),
      (RATE_LIMITER_HEADER_REMAINING, status.remainingRequests.toString)
    )
}
