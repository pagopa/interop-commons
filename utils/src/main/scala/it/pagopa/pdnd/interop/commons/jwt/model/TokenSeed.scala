package it.pagopa.pdnd.interop.commons.jwt.model

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID
import scala.util.Try

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
  purposes: String
)

object TokenSeed {
  def create(
    assertion: SignedJWT,
    key: JWK,
    audience: List[String],
    purposes: String,
    tokenIssuer: String,
    validityDuration: Long
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
      expireAt = issuedAt.plusMillis(validityDuration).toEpochMilli,
      audience = audience,
      purposes = purposes
    )

  }
}
