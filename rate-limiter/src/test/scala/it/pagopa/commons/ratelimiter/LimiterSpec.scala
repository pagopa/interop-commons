package it.pagopa.commons.ratelimiter

import it.pagopa.commons.ratelimiter.model.{LimiterConfig, TokenBucket}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.temporal.ChronoUnit
import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.duration._

class LimiterSpec extends AnyWordSpecLike {

  val configs: LimiterConfig = LimiterConfig(
    limiterGroup = "TEST",
    maxRequests = 200,
    burstPercentage = 1.2,
    rateInterval = 1.second,
    redisHost = "non-existing-host",
    redisPort = 6379
  )

//  def stubDateTimeSupplier(timestamp: OffsetDateTime): OffsetDateTimeSupplier = new OffsetDateTimeSupplier {
//    override def get: OffsetDateTime = timestamp
//  }

  val fakeDateTimeSupplier: OffsetDateTimeSupplier = new OffsetDateTimeSupplier {
    override def get: OffsetDateTime = throw new Exception("Fake functions should not be invoked")
  }

  val limiter: Limiter = Limiter(configs, fakeDateTimeSupplier)

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  "Bucket refill" should {
    "init with burst value" in {
      val rateInterval     = 1.minute
      val burstPercentage  = 1.5
      val limiter: Limiter =
        Limiter(configs.copy(rateInterval = rateInterval, burstPercentage = burstPercentage), fakeDateTimeSupplier)

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds * 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(limiter.burstRequests / 2, bucketTimestamp)

      val expected = TokenBucket(limiter.burstRequests, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "maintain burst value within time period" in {
      val rateInterval     = 1.minute
      val maxRequests      = 100
      val limiter: Limiter =
        Limiter(configs.copy(rateInterval = rateInterval, maxRequests = maxRequests), fakeDateTimeSupplier)

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(maxRequests * 2.0, bucketTimestamp)

      val expected = TokenBucket(currentBucket.tokens, now)
      limiter.refillBucket(currentBucket, now) shouldBe expected
    }

    "update tokens value if under burst within time period" in {
      val rateInterval     = 1.minute
      val maxRequests      = 100
      val burstPercentage  = 1.0
      val limiter: Limiter =
        Limiter(
          configs.copy(rateInterval = rateInterval, maxRequests = maxRequests, burstPercentage = burstPercentage),
          fakeDateTimeSupplier
        )

      val now             = timestamp
      val bucketTimestamp = timestamp.minus(rateInterval.toSeconds / 2, ChronoUnit.SECONDS)
      val currentBucket   = TokenBucket(1, bucketTimestamp)

      val expected = TokenBucket(51, now)

      limiter.refillBucket(currentBucket, now) shouldBe expected
    }
  }
}
