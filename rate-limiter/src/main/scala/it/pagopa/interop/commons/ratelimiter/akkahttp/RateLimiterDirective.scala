package it.pagopa.interop.commons.ratelimiter.akkahttp

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{onComplete, provide}
import akka.http.scaladsl.server.directives.RouteDirectives._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.ratelimiter.RateLimiter
import it.pagopa.interop.commons.ratelimiter.akkahttp.Errors.MissingOrganizationIdClaim
import it.pagopa.interop.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.commons.utils.TypeConversions._

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
    getClaim(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toUUID) match {
      case Success(orgId) =>
        implicit val c: Seq[(String, String)] = contexts
        onComplete(rateLimiter.rateLimiting(orgId)).flatMap {
          case Success(_)                  => provide(contexts)
          case Failure(_ @TooManyRequests) => complete(StatusCodes.TooManyRequests, tooManyRequestsProblem)
          // Never interrupt execution in case of unexpected rate limiting failure
          case Failure(err)                =>
            logger.error(s"Unexpected error during rate limiting for organization $orgId", err)
            provide(contexts)
        }

      case Failure(_) =>
        logger.error(s"Missing or not correctly formatted $ORGANIZATION_ID_CLAIM")(contexts)
        reject(MissingOrganizationIdClaim)
    }
}
