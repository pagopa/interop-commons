package it.pagopa.interop.commons.ratelimiter.impl

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

  override def rateLimiting(organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit] =
    executor.rateLimiting(organizationId)

}
