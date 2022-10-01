package it.pagopa.interop.commons.ratelimiter.model

import scala.concurrent.duration.FiniteDuration

final case class RateLimitStatus(maxRequests: Int, remainingRequests: Int, rateInterval: FiniteDuration)
