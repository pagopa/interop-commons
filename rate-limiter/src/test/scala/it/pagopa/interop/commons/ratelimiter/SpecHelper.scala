package it.pagopa.interop.commons.ratelimiter

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.interop.commons.ratelimiter.model.{LimiterConfig, RateLimitStatus}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalamock.scalatest.MockFactory

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends MockFactory {

  val configs: LimiterConfig = LimiterConfig(
    limiterGroup = "TEST",
    maxRequests = 200,
    burstPercentage = 1.2,
    rateInterval = 1.second,
    redisHost = "non-existing-host",
    redisPort = 6379,
    timeout = 2.seconds
  )

  val rateLimiterMock: RateLimiter                 = mock[RateLimiter]
  val cacheClientMock: CacheClient                 = mock[CacheClient]
  val dateTimeSupplierMock: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val rateLimitStatus: RateLimitStatus = RateLimitStatus(configs.maxRequests, configs.maxRequests, configs.rateInterval)

  def mockDateTimeSupplierGet(timestamp: OffsetDateTime) =
    (() => dateTimeSupplierMock.get()).expects().returning(timestamp).once()

  def mockCacheGetFailure() =
    (cacheClientMock
      .get(_: String)(_: ExecutionContext))
      .expects(*, *)
      .once()
      .returns(Future.failed(new Exception("Some Exception")))

  def mockCacheSetFailure() =
    (cacheClientMock
      .set(_: String, _: String)(_: ExecutionContext))
      .expects(*, *, *)
      .once()
      .returns(Future.failed(new Exception("Some Exception")))

  def mockCacheGet(key: String, result: Option[String]) =
    (cacheClientMock
      .get(_: String)(_: ExecutionContext))
      .expects(key, *)
      .once()
      .returns(Future.successful(result))

  def mockCacheDel(key: String) =
    (cacheClientMock
      .del(_: String)(_: ExecutionContext))
      .expects(key, *)
      .once()
      .returns(Future.successful(0L))

  def mockCacheSet(key: String, value: String) =
    (cacheClientMock
      .set(_: String, _: String)(_: ExecutionContext))
      .expects(key, value, *)
      .once()
      .returns(Future.successful(""))

  def mockRateLimiting(organizationId: UUID, result: RateLimitStatus) =
    (rateLimiterMock
      .rateLimiting(_: UUID)(
        _: ExecutionContext,
        _: LoggerTakingImplicit[ContextFieldsToLog],
        _: Seq[(String, String)]
      ))
      .expects(organizationId, *, *, *)
      .once()
      .returns(Future.successful(result))

  def mockRateLimitingFailure(organizationId: UUID) =
    (rateLimiterMock
      .rateLimiting(_: UUID)(
        _: ExecutionContext,
        _: LoggerTakingImplicit[ContextFieldsToLog],
        _: Seq[(String, String)]
      ))
      .expects(organizationId, *, *, *)
      .once()
      .returns(Future.failed(new Exception("Some Exception")))

  def mockRateLimitingTooManyRequests(organizationId: UUID) =
    (rateLimiterMock
      .rateLimiting(_: UUID)(
        _: ExecutionContext,
        _: LoggerTakingImplicit[ContextFieldsToLog],
        _: Seq[(String, String)]
      ))
      .expects(organizationId, *, *, *)
      .once()
      .returns(
        Future.failed(TooManyRequests(organizationId, RateLimitStatus(configs.maxRequests, 0, configs.rateInterval)))
      )
}
