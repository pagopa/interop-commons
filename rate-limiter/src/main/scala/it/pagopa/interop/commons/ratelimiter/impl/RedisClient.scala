package it.pagopa.interop.commons.ratelimiter.impl

import it.pagopa.interop.commons.ratelimiter.CacheClient
import redis.clients.jedis.JedisPooled

import scala.concurrent.{ExecutionContext, Future}

final case class RedisClient(client: JedisPooled) extends CacheClient {
  override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    Future(Option(client.get(key)))

  override def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String] =
    Future(client.set(key, value))

  override def del(key: String)(implicit ec: ExecutionContext): Future[Long] =
    Future(client.del(key))
}
