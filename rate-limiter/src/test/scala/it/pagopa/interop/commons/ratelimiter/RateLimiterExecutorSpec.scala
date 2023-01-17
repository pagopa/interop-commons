package it.pagopa.interop.commons.ratelimiter

import it.pagopa.interop.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.interop.commons.ratelimiter.model.{RateLimitStatus, TokenBucket}
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
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
        RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(
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
        RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(
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
        RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(
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
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
      val organizationId               = UUID.randomUUID()
      val bucket                       = TokenBucket(10.0, timestamp)

      mockCacheGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket.toJson.compactPrint))

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe bucket
    }

    "return new bucket if does not exist" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
      val organizationId               = UUID.randomUUID()

      val expected = TokenBucket(limiter.burstRequests, timestamp)

      mockCacheGet(limiter.key(configs.limiterGroup, organizationId), None)

      limiter.getBucket(configs.limiterGroup, organizationId, timestamp).futureValue shouldBe expected
    }
  }

  "Using token" should {
    "store bucket with used token" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
      val bucket                       = TokenBucket(10, timestamp)
      val organizationId               = UUID.randomUUID()
      val usedBucket                   = bucket.copy(tokens = bucket.tokens - 1)

      val expected = RateLimitStatus(configs.maxRequests, usedBucket.tokens.toInt, configs.rateInterval)

      mockCacheSet(limiter.key(configs.limiterGroup, organizationId), usedBucket.toJson.compactPrint)

      limiter.useToken(bucket, organizationId).futureValue shouldBe expected
    }

    "fail with Too Many Requests error if limit is exceeded" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
      val bucket                       = TokenBucket(0, timestamp)
      val organizationId               = UUID.randomUUID()

      val status = RateLimitStatus(configs.maxRequests, 0, configs.rateInterval)

      limiter.useToken(bucket, organizationId).failed.futureValue shouldBe TooManyRequests(organizationId, status)
    }
  }

  "Rate Limiting" should {
    "not fail" when {
      "current bucket retrieve fails" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
        val organizationId               = UUID.randomUUID()
        implicit val validContext: Seq[(String, String)] = Seq(ORGANIZATION_ID_CLAIM -> organizationId.toString)

        val expected = RateLimitStatus(configs.maxRequests, configs.maxRequests, configs.rateInterval)

        mockDateTimeSupplierGet(timestamp)
        mockCacheGetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe expected
      }

      "updated bucket store fails" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
        val organizationId               = UUID.randomUUID()
        val bucket                       = TokenBucket(10.0, timestamp).toJson.compactPrint
        implicit val validContext: Seq[(String, String)] = Seq(ORGANIZATION_ID_CLAIM -> organizationId.toString)

        val expected = RateLimitStatus(configs.maxRequests, configs.maxRequests, configs.rateInterval)

        mockDateTimeSupplierGet(timestamp)
        mockCacheGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))
        mockCacheSetFailure()

        limiter.rateLimiting(organizationId).futureValue shouldBe expected
      }
    }

    "fail" when {
      "limit has been exceeded" in {
        val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
        val organizationId               = UUID.randomUUID()
        val bucket                       = TokenBucket(0.0, timestamp).toJson.compactPrint
        implicit val validContext: Seq[(String, String)] = Seq(ORGANIZATION_ID_CLAIM -> organizationId.toString)

        val status = RateLimitStatus(configs.maxRequests, 0, configs.rateInterval)

        mockDateTimeSupplierGet(timestamp)
        mockCacheGet(limiter.key(configs.limiterGroup, organizationId), Some(bucket))

        limiter.rateLimiting(organizationId).failed.futureValue shouldBe TooManyRequests(organizationId, status)
      }
    }

    "delete a corrupted value if deserialization fails and generate a new bucket" in {
      val limiter: RateLimiterExecutor = RateLimiterExecutor(dateTimeSupplierMock, cacheClientMock)(configs)
      val organizationId               = UUID.randomUUID()
      val key                          = limiter.key(configs.limiterGroup, organizationId)
      implicit val validContext: Seq[(String, String)] = Seq(ORGANIZATION_ID_CLAIM -> organizationId.toString)

      val initialBucket        = TokenBucket(limiter.burstRequests, timestamp)
      val expectedStoredBucket = initialBucket.copy(tokens = initialBucket.tokens - 1)
      val expectedStatus = RateLimitStatus(configs.maxRequests, expectedStoredBucket.tokens.toInt, configs.rateInterval)

      mockDateTimeSupplierGet(timestamp)
      mockCacheGet(key, Some("unexpected-value"))
      mockCacheDel(key)
      mockCacheSet(limiter.key(configs.limiterGroup, organizationId), expectedStoredBucket.toJson.compactPrint)

      limiter.rateLimiting(organizationId).futureValue shouldBe expectedStatus
    }
  }

}
