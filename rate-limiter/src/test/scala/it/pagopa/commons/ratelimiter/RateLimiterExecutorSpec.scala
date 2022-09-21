package it.pagopa.commons.ratelimiter

import it.pagopa.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.commons.ratelimiter.model.TokenBucket
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json._

import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class RateLimiterExecutorSpec extends AnyWordSpecLike with SpecHelper {

  "Bucket refill" should {
    "init with burst value" in {
      val rateInterval                 = 1.minute
      val burstPercentage              = 1.5
      val limiter: RateLimiterExecutor =
        RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(
          configs.copy(rateInterval = rateInterval, burstPercentage = burstPercentage)
        )

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds * 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(limiter.burstRequests / 2, bucketTimestamp)

      val expected = TokenBucket(limiter.burstRequests, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "maintain burst value within time period" in {
      val rateInterval                 = 1.minute
      val maxRequests                  = 100
      val limiter: RateLimiterExecutor =
        RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(
          configs.copy(rateInterval = rateInterval, maxRequests = maxRequests)
        )

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(maxRequests * 2.0, bucketTimestamp)

      val expected = TokenBucket(currentBucket.tokens, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "update tokens value if under burst within time period" in {
      val rateInterval                 = 1.minute
      val maxRequests                  = 100
      val burstPercentage              = 1.0
      val limiter: RateLimiterExecutor =
        RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(
          configs.copy(rateInterval = rateInterval, maxRequests = maxRequests, burstPercentage = burstPercentage)
        )

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(1, bucketTimestamp)

      val expected = TokenBucket(51, now)

      limiter.refillBucket(currentBucket, now) shouldBe expected
    }
  }

  "Bucket retrieve" should {
    "return bucket if exists" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
      val organizationId               = UUID.randomUUID()
      val bucket                       = TokenBucket(10.0, timestamp)

      mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket.toJson.compactPrint))

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe bucket
    }

    "return new bucket if does not exist" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
      val organizationId               = UUID.randomUUID()

      val expected = TokenBucket(limiter.burstRequests, timestamp)

      mockRedisGet(limiter.key(configs.limiterGroup, organizationId), None)

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe expected
    }
  }

  "Using token" should {
    "store bucket with used token" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
      val bucket                       = TokenBucket(10, timestamp)
      val organizationId               = UUID.randomUUID()

      mockRedisSet(
        limiter.key(configs.limiterGroup, organizationId),
        bucket.copy(tokens = bucket.tokens - 1).toJson.compactPrint
      )

      limiter.useToken(bucket, organizationId).futureValue shouldBe ()
    }

    "fail with Too Many Requests error if limit is exceeded" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
      val bucket                       = TokenBucket(0, timestamp)
      val organizationId               = UUID.randomUUID()

      limiter.useToken(bucket, organizationId).failed.futureValue shouldBe TooManyRequests
    }
  }

  "Rate Limiting" should {
    "not fail" when {
      "current bucket retrieve fails" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
        val organizationId               = UUID.randomUUID()

        mockDateTimeSupplierGet(timestamp)
        mockRedisGetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe ()
      }

      "updated bucket store fails" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
        val organizationId               = UUID.randomUUID()
        val bucket                       = TokenBucket(10.0, timestamp).toJson.compactPrint

        mockDateTimeSupplierGet(timestamp)
        mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))
        mockRedisSetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe ()
      }
    }

    "fail" when {
      "limit has been exceeded" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
        val organizationId               = UUID.randomUUID()
        val bucket                       = TokenBucket(0.0, timestamp).toJson.compactPrint

        mockDateTimeSupplierGet(timestamp)
        mockRedisGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))

        limiter.rateLimiting(organizationId).failed.futureValue shouldBe TooManyRequests
      }
    }

    "delete a corrupted value if deserialization fails and generate a new bucket" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, redisClientMock)(configs)
      val organizationId               = UUID.randomUUID()
      val key                          = limiter.key(configs.limiterGroup, organizationId)

      val expected = TokenBucket(limiter.burstRequests, timestamp)

      mockDateTimeSupplierGet(timestamp)
      mockRedisGet(key, Some("unexpected-value"))
      mockRedisDel(key)
      mockRedisSet(
        limiter.key(configs.limiterGroup, organizationId),
        expected.copy(tokens = expected.tokens - 1).toJson.compactPrint
      )

      limiter.rateLimiting(organizationId).futureValue shouldBe ()
    }
  }

}
