package it.pagopa.interop.commons.ratelimiter

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait RateLimiter {
  def rateLimiting(organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit]
}
