package it.pagopa.interop.commons.ratelimiter.model

import scala.concurrent.duration.FiniteDuration

/**
  * Rate limiter configurations
  * @param limiterGroup Discerns rate limits, allowing to define different limits for different groups
  * @param maxRequests Max requests for each organization in the current group within the time interval 
  * @param burstPercentage How many requests, in percentage, can be made more than `maxRequests`. It allows burst requests. 
  *                        Notes: 
  *                         - once a burst has been made, the maximum number of requests in the same time interval will be `maxRequests`
  *                         - this value should be >= 1.0
  * @param rateInterval Rate limiting will be evaluated within this interval
  * @param redisHost Redis hostname
  * @param redisPort Redis port
  * @param timeout Timeframe connection and operations must be executed within
  *                Note: in case of timeout the request is executed without applying rate limit
  */
final case class LimiterConfig(
  limiterGroup: String,
  maxRequests: Int,
  burstPercentage: Double,
  rateInterval: FiniteDuration,
  redisHost: String,
  redisPort: Int,
  timeout: FiniteDuration
)
