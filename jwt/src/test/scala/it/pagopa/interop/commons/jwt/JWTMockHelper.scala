package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.crypto.{ECDSASigner, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.SeqHasAsJava

trait JWTMockHelper {
  val expirationTime: Date = Date.from(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)

  def createMockJWT(
    key: JWK,
    issuer: String,
    clientId: String,
    audience: List[String],
    algorithm: String,
    customClaims: Map[String, AnyRef] = Map.empty
  ): String = {
    val rsaKid        = key.computeThumbprint().toJSONString
    val privateRsaKey = key.toJSONString
    makeJWT(issuer, clientId, audience, expirationTime, algorithm, rsaKid, privateRsaKey, customClaims)
  }

  def makeJWT(
    issuer: String,
    clientId: String,
    audience: List[String],
    expirationTime: Date,
    algo: String,
    kid: String,
    privateKeyPEM: String,
    customClaims: Map[String, AnyRef] = Map.empty
  ): String = {
    val now = new Date()
    val jwk = JWK.parse(privateKeyPEM)

    // Create signer with the private key
    val signer = if (algo == "RSA") new RSASSASigner(jwk.toRSAKey.toPrivateKey) else new ECDSASigner(jwk.toECKey)

    val tempClaimsSet = new JWTClaimsSet.Builder()
      .issuer(issuer)
      .subject(clientId)
      .jwtID(UUID.randomUUID.toString)
      .audience(audience.asJava)
      .expirationTime(expirationTime)
      .issueTime(now)
      .notBeforeTime(now)

    customClaims.map { case (k, v) => tempClaimsSet.claim(k, v) }

    val claimsSet = tempClaimsSet.build()

    // Prepare JWS object with simple string as payload
    val algorithm = if (algo == "RSA") JWSAlgorithm.RS256 else JWSAlgorithm.ES256

    val jwsObject = new SignedJWT(
      new JWSHeader.Builder(algorithm)
        .keyID(kid)
        .build,
      claimsSet
    )

    // Sign
    jwsObject.sign(signer)
    jwsObject.serialize
  }

}
