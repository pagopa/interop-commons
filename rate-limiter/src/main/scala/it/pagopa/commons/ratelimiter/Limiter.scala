package it.pagopa.commons.ratelimiter

import cats.implicits._
import it.pagopa.commons.ratelimiter.error.Errors.TooManyRequests
import it.pagopa.commons.ratelimiter.model.{LimiterConfig, TokenBucket}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import redis.clients.jedis.JedisPooled
import spray.json._

import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

final case class Limiter(configs: LimiterConfig, dateTimeSupplier: OffsetDateTimeSupplier) {

  // TODO Logging
  // TODO It would be nice if the timestamp would be generated only once per run and used everywhere
  // TODO Set low connection timeout

//  val dateTimeSupplier: OffsetDateTimeSupplier = new OffsetDateTimeSupplier {
//    override def get: OffsetDateTime = OffsetDateTime.now()
//  }

//  val configs = LimiterConfig(
//    limiterGroup = "AUTH_SERVER",
//    maxRequests = 3,
//    burstPercentage = 1.2,
//    rateInterval = 1.second,
//    redisHost = "localhost",
//    redisPort = 6379
//  )

  private val jedis: JedisPooled = new JedisPooled(configs.redisHost, configs.redisPort)

  val burstRequests: Double = configs.maxRequests * configs.burstPercentage

//  val ec = ExecutionContext.global
//
//  println("STARTING")
//  Await.result(rateLimiting(UUID.fromString("7f2ecd7b-dc59-4f10-aeaf-f6b334d23512"))(ec), Duration.Inf)

  /**
    * Applies rate limiting using Token Bucket algorithm.
    * In case of any execution error, allows the request to avoid service outage
    */
  def rateLimiting(organizationId: UUID)(implicit ec: ExecutionContext): Future[Unit] = {
    val now    = dateTimeSupplier.get
    val result = for {
      bucket <- getBucket(configs.limiterGroup, organizationId, now)
      updatedBucket = refillBucket(bucket, now)
      _ <- useToken(updatedBucket, organizationId)
    } yield ()
    result
    // TODO Uncomment
//      .recoverWith {
//        case TooManyRequests => Future.failed(TooManyRequests)
//        case _               => Future.unit
//      }
  }

  def getBucket(component: String, organizationId: UUID, now: OffsetDateTime)(implicit
    ec: ExecutionContext
  ): Future[TokenBucket] =
    for {
      value  <- Future(Option(jedis.get(key(component, organizationId))))
      bucket <- value.fold(Future.successful(initBucket(now)))(b => Future(b.parseJson.convertTo[TokenBucket]))
      // TODO delete key on parsing error (to allow the next request to succeed)
    } yield bucket

  def storeBucket(component: String, organizationId: UUID, bucket: TokenBucket)(implicit
    ec: ExecutionContext
  ): Future[Unit] =
    Future(jedis.set(key(component, organizationId), bucket.toJson.compactPrint)).as(())

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

  def key(component: String, organizationId: UUID): String = s"${component}_$organizationId"
}
