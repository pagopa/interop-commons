package it.pagopa.interop.commons.ratelimiter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.commons.ratelimiter.akkahttp.Errors.MissingOrganizationIdClaim
import it.pagopa.interop.commons.ratelimiter.akkahttp.RateLimiterDirective._
import it.pagopa.interop.commons.ratelimiter.model.RateLimitStatus
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.DefaultJsonProtocol._

import java.util.UUID

class RateLimiterDirectiveSpec extends AnyWordSpecLike with SpecHelper with SprayJsonSupport with ScalatestRouteTest {

  val organizationId: UUID                = UUID.randomUUID()
  val validContext: Seq[(String, String)] = Seq(ORGANIZATION_ID_CLAIM -> organizationId.toString)

  val errorResponse = "Error"

  "Rate Limiter Directive" should {
    "allow request under rate limit" in {
      val directive: Directive1[Seq[(String, String)]] =
        rateLimiterDirective(rateLimiterMock, errorResponse)(validContext)

      val rateLimitStatus = RateLimitStatus(configs.maxRequests, configs.maxRequests / 2, configs.rateInterval)

      val expectedHeaders = Seq(
        RawHeader("X-RateLimit-Limit", rateLimitStatus.maxRequests.toString),
        RawHeader("X-RateLimit-Remaining", rateLimitStatus.remainingRequests.toString),
        RawHeader("X-RateLimit-Interval", rateLimitStatus.rateInterval.toMillis.toString)
      )

      mockRateLimiting(organizationId, rateLimitStatus)

      Get() ~> directive { str => complete(StatusCodes.OK, str) } ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[(String, String)]] should contain allElementsOf validContext
        headers should contain allElementsOf expectedHeaders
      }
    }

    "block request exceeding rate limit" in {
      val directive: Directive1[Seq[(String, String)]] =
        rateLimiterDirective(rateLimiterMock, errorResponse)(validContext)

      val expectedStatus = RateLimitStatus(configs.maxRequests, 0, configs.rateInterval)

      val expectedHeaders = Seq(
        RawHeader("X-RateLimit-Limit", expectedStatus.maxRequests.toString),
        RawHeader("X-RateLimit-Remaining", expectedStatus.remainingRequests.toString),
        RawHeader("X-RateLimit-Interval", expectedStatus.rateInterval.toMillis.toString)
      )

      mockRateLimitingTooManyRequests(organizationId)

      Get() ~> directive { str => complete(StatusCodes.OK, str) } ~> check {
        status shouldBe StatusCodes.TooManyRequests
        responseAs[String] shouldBe errorResponse
        headers should contain allElementsOf expectedHeaders
      }
    }

    "allow request on rate limiting failure" in {
      val directive: Directive1[Seq[(String, String)]] =
        rateLimiterDirective(rateLimiterMock, errorResponse)(validContext)

      mockRateLimitingFailure(organizationId)

      Get() ~> directive { str => complete(StatusCodes.OK, str) } ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[(String, String)]] shouldBe validContext
      }
    }

    "block request if organization id is not found" in {
      val directive: Directive1[Seq[(String, String)]] =
        rateLimiterDirective(rateLimiterMock, errorResponse)(Nil)

      Get() ~> directive { str => complete(StatusCodes.OK, str) } ~> check {
        rejection shouldBe MissingOrganizationIdClaim
      }
    }
  }

}