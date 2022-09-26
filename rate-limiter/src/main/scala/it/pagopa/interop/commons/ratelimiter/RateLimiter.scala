package it.pagopa.interop.commons.ratelimiter

import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.ratelimiter.model.RateLimitStatus

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait RateLimiter {
  def rateLimiting(organizationId: UUID)(implicit
    ec: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    contexts: Seq[(String, String)]
  ): Future[RateLimitStatus]
}
