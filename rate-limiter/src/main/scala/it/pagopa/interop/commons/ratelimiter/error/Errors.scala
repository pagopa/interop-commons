package it.pagopa.interop.commons.ratelimiter.error

import it.pagopa.interop.commons.ratelimiter.model.RateLimitStatus

import java.util.UUID

object Errors {
  final case class TooManyRequests(tenantId: UUID, status: RateLimitStatus)
      extends Throwable(s"Too many requests for organization $tenantId")
  final case class DeserializationFailed(key: String) extends Throwable(s"Deserialization failed for key $key")
}
