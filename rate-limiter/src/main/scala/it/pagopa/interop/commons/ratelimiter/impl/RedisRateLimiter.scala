package it.pagopa.interop.commons.ratelimiter.impl

import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.ratelimiter.model.LimiterConfig
import it.pagopa.interop.commons.ratelimiter.{RateLimiter, RateLimiterExecutor}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import redis.clients.jedis.JedisPooled

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class RedisRateLimiter(configs: LimiterConfig, dateTimeSupplier: OffsetDateTimeSupplier)
    extends RateLimiter {

  // TODO Set low connection timeout

  private val jedis: JedisPooled = new JedisPooled(configs.redisHost, configs.redisPort)

  private val executor = RateLimiterExecutor(dateTimeSupplier, RedisClient(jedis))(configs)

  override def rateLimiting(organizationId: UUID)(implicit
    ec: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    contexts: Seq[(String, String)]
  ): Future[Unit] =
    executor.rateLimiting(organizationId)

}
