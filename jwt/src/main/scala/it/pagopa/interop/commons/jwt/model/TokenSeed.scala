package it.pagopa.interop.commons.jwt.model

import com.nimbusds.jose.JWSAlgorithm
import it.pagopa.interop.commons.jwt.{INTERNAL_ROLES, clientIdClaim}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

/** Data model for JWT tokens
  * @param id token identifier claim - <code>jit</code>
  * @param algorithm algorithm used for this token
  * @param kid key identifier
  * @param issuer token issuer claim - <code>iss</code>
  * @param issuedAt token issued at claim - <code>iat</code>
  * @param nbf token not before claim - <code>nbf</code>
  * @param expireAt token expiration date claim - <code>exp</code>
  * @param audience token audience claim - <code>aud</code>
  * @param customClaims token custom claims map
  */
final case class TokenSeed(
  id: UUID,
  algorithm: JWSAlgorithm,
  kid: String,
  subject: String,
  issuer: String,
  issuedAt: Long,
  nbf: Long,
  expireAt: Long,
  audience: List[String],
  customClaims: Map[String, AnyRef]
)

/** Singleton for <code>TokenSeed</code> instances constructions.
  */
object TokenSeed {

  def createInternalTokenWithKid(
    algorithm: JWSAlgorithm,
    kid: String,
    subject: String,
    audience: List[String],
    tokenIssuer: String,
    validityDurationSeconds: Long
  ): TokenSeed = {
    val issuedAt = Instant.now(Clock.system(ZoneId.of("UTC")))
    TokenSeed(
      id = UUID.randomUUID(),
      algorithm = algorithm,
      kid = kid,
      subject = subject,
      issuer = tokenIssuer,
      issuedAt = issuedAt.toEpochMilli,
      nbf = issuedAt.toEpochMilli,
      expireAt = issuedAt.plus(validityDurationSeconds, ChronoUnit.SECONDS).toEpochMilli,
      audience = audience,
      customClaims = INTERNAL_ROLES
    )
  }

  def createWithKid(
    algorithm: JWSAlgorithm,
    subject: String,
    kid: String,
    audience: List[String],
    customClaims: Map[String, AnyRef],
    tokenIssuer: String,
    validityDurationSeconds: Long
  ): TokenSeed = {
    val issuedAt = Instant.now(Clock.system(ZoneId.of("UTC")))

    TokenSeed(
      id = UUID.randomUUID(),
      algorithm = algorithm,
      kid = kid,
      subject = subject,
      issuer = tokenIssuer,
      issuedAt = issuedAt.toEpochMilli,
      nbf = issuedAt.toEpochMilli,
      expireAt = issuedAt.plus(validityDurationSeconds, ChronoUnit.SECONDS).toEpochMilli,
      audience = audience,
      customClaims = customClaims + (clientIdClaim -> subject)
    )
  }
}
