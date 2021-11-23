package it.pagopa.pdnd.interop.commons.jwt.model

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID
import scala.util.Try

/** Data model for JWT tokens
  * @param id token identifier claim - <code>jit</code>
  * @param algorithm algorithm used for this token
  * @param kid key identifier
  * @param clientId token subject claim - <code>sub</code>
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
  clientId: String,
  issuer: String,
  issuedAt: Long,
  nbf: Long,
  expireAt: Long,
  audience: List[String],
  customClaims: Map[String, String]
)

/** Singleton for <code>TokenSeed</code> instances constructions.
  */
object TokenSeed {

  /** Returns a <code>TokenSeed</code>
    * @param assertion original client assertion JWT
    * @param key key used for token signature
    * @param audience <code>aud</code> claim content
    * @param customClaims map of possible custom claims in string format
    * @param tokenIssuer <code>iss</code> claim content
    * @param validityDurationMilliseconds milliseconds representing this token duration
    * @return
    */
  def create(
    assertion: SignedJWT,
    key: JWK,
    audience: List[String],
    customClaims: Map[String, String],
    tokenIssuer: String,
    validityDurationMilliseconds: Long
  ): Try[TokenSeed] = Try {
    val issuedAt = Instant.now(Clock.system(ZoneId.of("UTC")))
    TokenSeed(
      id = UUID.randomUUID(),
      algorithm = assertion.getHeader.getAlgorithm,
      kid = key.computeThumbprint().toString,
      clientId = assertion.getJWTClaimsSet.getSubject,
      issuer = tokenIssuer,
      issuedAt = issuedAt.toEpochMilli,
      nbf = issuedAt.toEpochMilli,
      expireAt = issuedAt.plusMillis(validityDurationMilliseconds).toEpochMilli,
      audience = audience,
      customClaims = customClaims
    )

  }
}
