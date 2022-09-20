package it.pagopa.commons.ratelimiter

import it.pagopa.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.commons.ratelimiter.model.{LimiterConfig, TokenBucket}
import it.pagopa.commons.ratelimiter.utils.RedisClient
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.temporal.ChronoUnit
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import spray.json._

class LimiterExecutorSpec extends AnyWordSpecLike with MockFactory {

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

//  def redisClientSuccessfulStub(retrievedValue: Option[String]): RedisClient = new RedisClient {
//    override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] =
//      Future.successful(retrievedValue)
//
//    override def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String] = Future.successful("")
//  }
//
//  val redisClientSuccessfulStub: RedisClient = redisClientSuccessfulStub(Some(""))
//
//  val redisClientFailureStub: RedisClient = new RedisClient {
//    override def get(key: String)(implicit ec: ExecutionContext): Future[Option[String]] =
//      Future.failed(new Exception("Some exception"))
//
//    override def set(key: String, value: String)(implicit ec: ExecutionContext): Future[String] =
//      Future.failed(new Exception("Some exception"))
//  }

//  val fakeDateTimeSupplier: OffsetDateTimeSupplier = new OffsetDateTimeSupplier {
//    override def get: OffsetDateTime = throw new Exception("Fake functions should not be invoked")
//  }

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  "Bucket refill" should {
    "init with burst value" in {
      val rateInterval             = 1.minute
      val burstPercentage          = 1.5
      val limiter: LimiterExecutor =
        LimiterExecutor(
          configs.copy(rateInterval = rateInterval, burstPercentage = burstPercentage),
          dateTimeSupplierMock
        )(redisClientMock)

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds * 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(limiter.burstRequests / 2, bucketTimestamp)

      val expected = TokenBucket(limiter.burstRequests, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "maintain burst value within time period" in {
      val rateInterval             = 1.minute
      val maxRequests              = 100
      val limiter: LimiterExecutor =
        LimiterExecutor(configs.copy(rateInterval = rateInterval, maxRequests = maxRequests), dateTimeSupplierMock)(
          redisClientMock
        )

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(maxRequests * 2.0, bucketTimestamp)

      val expected = TokenBucket(currentBucket.tokens, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "update tokens value if under burst within time period" in {
      val rateInterval             = 1.minute
      val maxRequests              = 100
      val burstPercentage          = 1.0
      val limiter: LimiterExecutor =
        LimiterExecutor(
          configs.copy(rateInterval = rateInterval, maxRequests = maxRequests, burstPercentage = burstPercentage),
          dateTimeSupplierMock
        )(redisClientMock)

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(1, bucketTimestamp)

      val expected = TokenBucket(51, now)

      limiter.refillBucket(currentBucket, now) shouldBe expected
    }
  }

  "Bucket retrieve" should {
    "return bucket if exists" in {
      val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
      val organizationId           = UUID.randomUUID()
      val bucket                   = TokenBucket(10.0, timestamp)

      mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket.toJson.compactPrint))

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe bucket
    }

    "return new bucket if does not exist" in {
      val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
      val organizationId           = UUID.randomUUID()

      val expected = TokenBucket(limiter.burstRequests, timestamp)

      mockRedisGet(limiter.key(configs.limiterGroup, organizationId), None)

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe expected
    }
  }

  "Using token" should {
    "store bucket with used token" in {
      val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
      val bucket                   = TokenBucket(10, timestamp)
      val organizationId           = UUID.randomUUID()

      (redisClientMock
        .set(_: String, _: String)(_: ExecutionContext))
        .expects(
          limiter.key(configs.limiterGroup, organizationId),
          bucket.copy(tokens = bucket.tokens - 1).toJson.compactPrint,
          *
        )
        .once()
        .returns(Future.successful(""))

      limiter.useToken(bucket, organizationId).futureValue shouldBe ()
    }

    "fail with Too Many Requests error if limit is exceeded" in {
      val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
      val bucket                   = TokenBucket(0, timestamp)
      val organizationId           = UUID.randomUUID()

      limiter.useToken(bucket, organizationId).failed.futureValue shouldBe TooManyRequests
    }
  }

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

  "Rate Limiting" should {
    "not fail" when {
      "current bucket retrieve fails" in {
        val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
        val organizationId           = UUID.randomUUID()

        (() => dateTimeSupplierMock.get).expects().returning(timestamp).once()
        mockRedisGetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe ()
      }

      "updated bucket store fails" in {
        val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
        val organizationId           = UUID.randomUUID()
        val bucket                   = TokenBucket(10.0, timestamp).toJson.compactPrint

        (() => dateTimeSupplierMock.get).expects().returning(timestamp).once()
        mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))
        mockRedisSetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe ()
      }
    }

    "fail" when {
      "limit has been exceeded" in {
        val limiter: LimiterExecutor = LimiterExecutor(configs, dateTimeSupplierMock)(redisClientMock)
        val organizationId           = UUID.randomUUID()
        val bucket                   = TokenBucket(0.0, timestamp).toJson.compactPrint

        (() => dateTimeSupplierMock.get).expects().returning(timestamp).once()
        mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))

        limiter.rateLimiting(organizationId).failed.futureValue shouldBe TooManyRequests
      }
    }

  }

  "Bucket conversion failure" should {
    "delete the corrupted key" in {}
  }
}
