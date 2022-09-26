package it.pagopa.interop.commons.ratelimiter.error

import it.pagopa.interop.commons.ratelimiter.model.RateLimitStatus

object Errors {
  final case class TooManyRequests(status: RateLimitStatus) extends Throwable("Too many requests")
  final case class DeserializationFailed(key: String)       extends Throwable(s"Deserialization failed for key $key")
}
