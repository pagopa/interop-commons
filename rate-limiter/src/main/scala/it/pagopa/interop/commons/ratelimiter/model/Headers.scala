package it.pagopa.interop.commons.ratelimiter.model

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import it.pagopa.interop.commons.utils.AkkaUtils.getClaim

object Headers {

  final val RATE_LIMITER_HEADER_LIMIT     = "X-RateLimit-Limit"
  final val RATE_LIMITER_HEADER_INTERVAL  = "X-RateLimit-Interval"
  final val RATE_LIMITER_HEADER_REMAINING = "X-RateLimit-Remaining"

  def headersFromStatus(status: RateLimitStatus): List[HttpHeader] =
    List(
      RawHeader(RATE_LIMITER_HEADER_LIMIT, status.maxRequests.toString),
      RawHeader(RATE_LIMITER_HEADER_INTERVAL, status.rateInterval.toMillis.toString),
      RawHeader(RATE_LIMITER_HEADER_REMAINING, status.remainingRequests.toString)
    )

  def headersFromContext(contexts: Seq[(String, String)]): List[HttpHeader] =
    List(
      header(contexts, RATE_LIMITER_HEADER_LIMIT),
      header(contexts, RATE_LIMITER_HEADER_INTERVAL),
      header(contexts, RATE_LIMITER_HEADER_REMAINING)
    ).flatten

  private def header(contexts: Seq[(String, String)], headerName: String): Option[RawHeader] =
    getClaim(contexts, headerName).toOption.map(RawHeader(headerName, _))
}
