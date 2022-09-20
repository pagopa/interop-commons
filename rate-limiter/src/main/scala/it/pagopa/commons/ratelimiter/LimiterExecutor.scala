package it.pagopa.commons.ratelimiter

import cats.implicits._
import it.pagopa.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.commons.ratelimiter.model.{LimiterConfig, TokenBucket}
import it.pagopa.commons.ratelimiter.utils.RedisClient
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import spray.json._

import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

private[ratelimiter] final case class LimiterExecutor(configs: LimiterConfig, dateTimeSupplier: OffsetDateTimeSupplier)(
  redisClient: RedisClient
) {
  // TODO Logging

  val burstRequests: Double = configs.maxRequests * configs.burstPercentage

  /**
    * Applies rate limiting using Token Bucket algorithm.
    * In case of any execution error, allows the request to avoid service outage
    */
  def rateLimiting(organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit] = {
    val now    = dateTimeSupplier.get
    val result = for {
      // Note: this is not transactional. Potentially N requests can use just 1 token
      bucket <- getBucket(configs.limiterGroup, organizationId, now)
      updatedBucket = refillBucket(bucket, now)
      _ <- useToken(updatedBucket, organizationId)
    } yield ()
    result
      .recoverWith {
        case TooManyRequests => Future.failed(TooManyRequests)
        case _               => Future.unit
      }
  }

  def getBucket(limiterGroup: String, organizationId: UUID, now: OffsetDateTime)(implicit
    ec: ExecutionContext
  ): Future[TokenBucket] =
    for {
      value  <- redisClient.get(key(limiterGroup, organizationId))
      bucket <- value.fold(Future.successful(initBucket(now)))(b => Future(b.parseJson.convertTo[TokenBucket]))
      // TODO delete key on parsing error (to allow the next request to succeed)
    } yield bucket

  def storeBucket(limiterGroup: String, organizationId: UUID, bucket: TokenBucket)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    redisClient.set(key(limiterGroup, organizationId), bucket.toJson.compactPrint).as(())

  def useToken(bucket: TokenBucket, organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit] =
    if (bucket.tokens >= 1)
      storeBucket(configs.limiterGroup, organizationId, bucket.copy(tokens = bucket.tokens - 1))
    else
      Future.failed(TooManyRequests)

  def refillBucket(bucket: TokenBucket, now: OffsetDateTime): TokenBucket = {
    val updatedTokens = releaseTokens(bucket, now)
    TokenBucket(tokens = updatedTokens, lastCall = now)
  }

  // TODO Rename
  def releaseTokens(currentBucket: TokenBucket, now: OffsetDateTime): Double = {
    val elapsed = now.toMillis - currentBucket.lastCall.toMillis
    if (FiniteDuration(elapsed, TimeUnit.MILLISECONDS) > configs.rateInterval) {
      burstRequests
    } else if (currentBucket.tokens > configs.maxRequests) // Allows burst
      currentBucket.tokens
    else {
      val timeRefill = elapsed.toDouble * configs.maxRequests / configs.rateInterval.toMillis
      Math.min(currentBucket.tokens + timeRefill, configs.maxRequests.toDouble)
    }
  }

  def initBucket(now: OffsetDateTime): TokenBucket = TokenBucket(tokens = burstRequests, lastCall = now)

  def key(limiterGroup: String, organizationId: UUID): String = s"${limiterGroup}_$organizationId"
}
