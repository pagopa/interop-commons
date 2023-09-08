package it.pagopa.interop.commons.ratelimiter.akkahttp

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
import it.pagopa.interop.commons.utils.errors.AkkaResponses.tooManyRequests
import it.pagopa.interop.commons.utils.errors.{GenericComponentErrors, ServiceCode}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object RateLimiterDirective {

  def rateLimiterDirective(rateLimiter: RateLimiter)(contexts: Seq[(String, String)])(implicit
    ec: ExecutionContext,
    serviceCode: ServiceCode,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Directive1[Seq[(String, String)]] = rateLimit(rateLimiter)(contexts).flatMap(addHeaders)

  private def rateLimit(rateLimiter: RateLimiter)(contexts: Seq[(String, String)])(implicit
    ec: ExecutionContext,
    serviceCode: ServiceCode,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Directive1[Seq[(String, String)]] =
    getOrganizationIdUUID(contexts) match {
      case Right(orgId) =>
        implicit val c: Seq[(String, String)] = contexts
        onComplete(rateLimiter.rateLimiting(orgId)).flatMap {
          case Success(status)               => provide(contexts ++ statusToContext(status))
          case Failure(tmr: TooManyRequests) =>
            tooManyRequests(
              GenericComponentErrors.TooManyRequests,
              s"Requests limit exceeded for organization $orgId",
              Headers.headersFromStatus(tmr.status)
            )
          // Never interrupt execution in case of unexpected rate limiting failure
          case Failure(err)                  =>
            logger.warn(s"Unexpected error during rate limiting for organization $orgId", err)
            provide(contexts)
        }

      case Left(_) =>
        logger.warn(s"Missing or not correctly formatted $ORGANIZATION_ID_CLAIM")(contexts)
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
