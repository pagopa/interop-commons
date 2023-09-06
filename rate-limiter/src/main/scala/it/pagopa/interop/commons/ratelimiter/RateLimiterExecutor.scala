package it.pagopa.interop.commons.ratelimiter

import cats.implicits._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.ratelimiter.error.Errors.{DeserializationFailed, TooManyRequests}
import it.pagopa.interop.commons.ratelimiter.model.{LimiterConfig, RateLimitStatus, TokenBucket}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import spray.json._

import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

private[ratelimiter] final case class RateLimiterExecutor(
  dateTimeSupplier: OffsetDateTimeSupplier,
  cacheClient: CacheClient
)(configs: LimiterConfig) {

  val burstRequests: Double = configs.maxRequests * configs.burstPercentage

  /**
    * Applies rate limiting using Token Bucket algorithm.
    * In case of any execution error, allows the request to avoid service outage
    */
  def rateLimiting(organizationId: UUID)(implicit
    ec: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    contexts: Seq[(String, String)]
  ): Future[RateLimitStatus] = {
    val now    = dateTimeSupplier.get()
    val result = for {
      // Note: this is not transactional. Potentially N requests can use just 1 token
      bucket <- getBucket(configs.limiterGroup, organizationId, now).recoverWith(clearOnDeserializationError(now))
      refilledBucket = refillBucket(bucket, now)
      status <- useToken(refilledBucket, organizationId)
    } yield status
    result
      .recoverWith {
        case tmr: TooManyRequests =>
          logger.warn(s"Rate Limit triggered for organization $organizationId")
          Future.failed(tmr)
        case err                  =>
          logger.warn(s"Unexpected error during rate limiting for organization $organizationId", err)
          Future.successful(RateLimitStatus(configs.maxRequests, configs.maxRequests, configs.rateInterval))
      }
  }

  def getBucket(limiterGroup: String, organizationId: UUID, now: OffsetDateTime)(implicit
    ec: ExecutionContext
  ): Future[TokenBucket] =
    for {
      key    <- Future.successful(key(limiterGroup, organizationId))
      value  <- cacheClient.get(key)
      bucket <- value.fold(Future.successful(initBucket(now)))(parseValue(key, _))
    } yield bucket

  def parseValue(key: String, serializedBucket: String)(implicit ec: ExecutionContext): Future[TokenBucket] =
    Future(serializedBucket.parseJson.convertTo[TokenBucket])
      .recoverWith(_ => Future.failed(DeserializationFailed(key)))

  // If parsing fails, the value is removed from the cache
  // This ensures that in case of bugs or models changes, not manual maintenance is required
  //   and the rate limit logic resumes on the next run
  def clearOnDeserializationError(now: OffsetDateTime)(implicit
    ec: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    contexts: Seq[(String, String)]
  ): PartialFunction[Throwable, Future[TokenBucket]] = { case DeserializationFailed(key) =>
    logger.warn(s"Deserialization failed for key $key")
    cacheClient.del(key).as(initBucket(now))
  }

  def storeBucket(limiterGroup: String, organizationId: UUID, bucket: TokenBucket)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    cacheClient.set(key(limiterGroup, organizationId), bucket.toJson.compactPrint).as(())

  def useToken(bucket: TokenBucket, organizationId: UUID)(implicit ec: ExecutionContext): Future[RateLimitStatus] =
    if (bucket.tokens >= 1) {
      val updatedToken = bucket.copy(tokens = bucket.tokens - 1)
      storeBucket(configs.limiterGroup, organizationId, updatedToken)
        .as(RateLimitStatus(configs.maxRequests, updatedToken.tokens.toInt, configs.rateInterval))
    } else
      Future.failed(
        TooManyRequests(
          tenantId = organizationId,
          status = RateLimitStatus(configs.maxRequests, 0, configs.rateInterval)
        )
      )

  def refillBucket(bucket: TokenBucket, now: OffsetDateTime): TokenBucket = {
    val updatedTokens = releaseTokens(bucket, now)
    TokenBucket(tokens = updatedTokens, lastCall = now)
  }

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
