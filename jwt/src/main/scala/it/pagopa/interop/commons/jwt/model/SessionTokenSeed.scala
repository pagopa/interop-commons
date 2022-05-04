package it.pagopa.interop.commons.jwt.model

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.UUID
import scala.util.Try

/** Data model for Session Token
  * @param id
  * @param algorithm
  * @param kid
  * @param issuer
  * @param issuedAt
  * @param notBefore
  * @param expireAt
  * @param audience
  * @param customClaims
  */
final case class SessionTokenSeed(
  id: UUID,
  algorithm: JWSAlgorithm,
  kid: String,
  issuer: String,
  issuedAt: Long,
  notBefore: Long,
  expireAt: Long,
  audience: Set[String],
  claimsSet: Map[String, AnyRef]
)

object SessionTokenSeed {

  def create(
    claimsSet: Map[String, AnyRef],
    key: JWK,
    algorithm: JWSAlgorithm,
    audience: Set[String],
    tokenIssuer: String,
    validityDurationSeconds: Long
  ): Try[SessionTokenSeed] = {
    Try {
      val kid: String       = key.computeThumbprint().toString
      val issuedAt: Instant = Instant.now(Clock.system(ZoneId.of("UTC")))
      val iat: Long         = issuedAt.toEpochMilli
      val nbf: Long         = iat
      val exp: Long         = issuedAt.plus(validityDurationSeconds, ChronoUnit.SECONDS).toEpochMilli

      SessionTokenSeed(
        id = UUID.randomUUID(),
        algorithm = algorithm,
        kid = kid,
        issuer = tokenIssuer,
        issuedAt = iat,
        notBefore = nbf,
        expireAt = exp,
        audience = audience,
        claimsSet = claimsSet
      )
    }

  }

  def createWithKid(
    claimsSet: Map[String, AnyRef],
    kid: String,
    algorithm: JWSAlgorithm,
    audience: Set[String],
    tokenIssuer: String,
    validityDurationSeconds: Long
  ): Try[SessionTokenSeed] = {
    Try {
      val issuedAt: Instant = Instant.now(Clock.system(ZoneId.of("UTC")))
      val iat: Long         = issuedAt.toEpochMilli
      val nbf: Long         = iat
      val exp: Long         = issuedAt.plus(validityDurationSeconds, ChronoUnit.SECONDS).toEpochMilli

      SessionTokenSeed(
        id = UUID.randomUUID(),
        algorithm = algorithm,
        kid = kid,
        issuer = tokenIssuer,
        issuedAt = iat,
        notBefore = nbf,
        expireAt = exp,
        audience = audience,
        claimsSet = claimsSet
      )
    }
  }
}
