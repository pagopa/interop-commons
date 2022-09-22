package it.pagopa.interop.commons.ratelimiter

import it.pagopa.interop.commons.ratelimiter.model.LimiterConfig
import it.pagopa.interop.commons.ratelimiter.utils.RedisClient
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalamock.scalatest.MockFactory

import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends MockFactory {

  val configs: LimiterConfig = LimiterConfig(
    limiterGroup = "TEST",
    maxRequests = 200,
    burstPercentage = 1.2,
    rateInterval = 1.second,
    redisHost = "non-existing-host",
    redisPort = 6379
  )

  val redisClientMock: RedisClient                 = mock[RedisClient]
  val dateTimeSupplierMock: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  def mockDateTimeSupplierGet(timestamp: OffsetDateTime) =
    (() => dateTimeSupplierMock.get).expects().returning(timestamp).once()

  def mockRedisGetFailure() =
    (redisClientMock
      .get(_: String)(_: ExecutionContext))
      .expects(*, *)
      .once()
      .returns(Future.failed(new Exception("Some Exception")))

  def mockRedisSetFailure() =
    (redisClientMock
      .set(_: String, _: String)(_: ExecutionContext))
      .expects(*, *, *)
      .once()
      .returns(Future.failed(new Exception("Some Exception")))

  def mockRedisGet(key: String, result: Option[String]) =
    (redisClientMock
      .get(_: String)(_: ExecutionContext))
      .expects(key, *)
      .once()
      .returns(Future.successful(result))

  def mockRedisDel(key: String) =
    (redisClientMock
      .del(_: String)(_: ExecutionContext))
      .expects(key, *)
      .once()
      .returns(Future.successful(0L))

  def mockRedisSet(key: String, value: String) =
    (redisClientMock
      .set(_: String, _: String)(_: ExecutionContext))
      .expects(key, value, *)
      .once()
      .returns(Future.successful(""))

}
