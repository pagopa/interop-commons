package it.pagopa.commons.ratelimiter.akka

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{onComplete, provide}
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server.{Directive1, Rejection}
import it.pagopa.commons.ratelimiter.RateLimiter
import it.pagopa.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.commons.utils.TypeConversions._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object RateLimiterDirective {

  object MissingOrganizationIdClaim
      extends Throwable(s"Missing expected $ORGANIZATION_ID_CLAIM claim in token")
      with Rejection

  def rateLimiterDirective[T](rateLimiter: RateLimiter, tooManyRequestsProblem: T)(
    contexts: Seq[(String, String)]
  )(implicit
    ec: ExecutionContext,
    toEntityMarshallerProblem: ToEntityMarshaller[T]
  ): Directive1[Seq[(String, String)]] =
    getClaim(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toUUID) match {
      case Success(orgId) =>
        onComplete(rateLimiter.rateLimiting(orgId)).flatMap {
          case Success(_)                  => provide(contexts)
          case Failure(_ @TooManyRequests) =>
            complete(StatusCodes.TooManyRequests, tooManyRequestsProblem)
          // Never interrupt execution in case of unexpected rate limiting failure
          case Failure(_)                  => provide(contexts)
        }

      case Failure(_) => reject(MissingOrganizationIdClaim)
    }
}
