package it.pagopa.commons.ratelimiter.utils

import redis.clients.jedis.JedisPooled

import scala.concurrent.{ExecutionContext, Future}

trait RedisClient {
  def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]]
  def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String]
}

final case class JedisClient(client: JedisPooled) extends RedisClient {
  override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    Future(Option(client.get(key)))

  override def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String] =
    Future(client.set(key, value))
}
