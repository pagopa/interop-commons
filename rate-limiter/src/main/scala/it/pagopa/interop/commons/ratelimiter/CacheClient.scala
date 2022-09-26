package it.pagopa.interop.commons.ratelimiter

import scala.concurrent.{ExecutionContext, Future}

trait CacheClient {
  def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]]
  def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String]
  def del(key: String)(implicit ec: ExecutionContext): Future[Long]
}
