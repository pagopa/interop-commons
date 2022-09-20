package it.pagopa.commons.ratelimiter

import it.pagopa.commons.ratelimiter.model.LimiterConfig
import it.pagopa.commons.ratelimiter.utils.JedisClient
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import redis.clients.jedis.JedisPooled

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class Limiter(configs: LimiterConfig, dateTimeSupplier: OffsetDateTimeSupplier) {

  // TODO Set low connection timeout

  private val jedis: JedisPooled = new JedisPooled(configs.redisHost, configs.redisPort)

  private val executor = LimiterExecutor(dateTimeSupplier, JedisClient(jedis))(configs)

  def rateLimiting(organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit] =
    executor.rateLimiting(organizationId)

}
