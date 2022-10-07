package it.pagopa.interop.commons.ratelimiter.model

import it.pagopa.interop.commons.utils.SprayCommonFormats._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import java.time.OffsetDateTime

case class TokenBucket(tokens: Double, lastCall: OffsetDateTime)

object TokenBucket {
  implicit val tokenBucketFormat: RootJsonFormat[TokenBucket] = jsonFormat2(TokenBucket.apply)
}
