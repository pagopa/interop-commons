package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.{ECDSASigner, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.SeqHasAsJava

trait JWTMockHelper {

  def createMockJWT(key: JWK, issuer: String, clientId: String, audiences: List[String], algorithm: String): String = {
    val rsaKid         = key.computeThumbprint().toJSONString
    val privateRsaKey  = key.toJSONString
    val expirationTime = Date.from(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
    makeJWT(issuer, clientId, audiences, expirationTime, algorithm, rsaKid, privateRsaKey)
  }

  def makeJWT(
    issuer: String,
    clientId: String,
    audiences: List[String],
    expirationTime: Date,
    algo: String,
    kid: String,
    privateKeyPEM: String
  ): String = {
    val now = new Date()
    val jwk = JWK.parse(privateKeyPEM)

    // Create signer with the private key
    val signer = if (algo == "RSA") new RSASSASigner(jwk.toRSAKey.toPrivateKey) else new ECDSASigner(jwk.toECKey)

    val claimsSet = new JWTClaimsSet.Builder()
      .issuer(issuer)
      .subject(clientId)
      .jwtID(UUID.randomUUID.toString)
      .audience(audiences.asJava)
      .expirationTime(expirationTime)
      .issueTime(now)
      .notBeforeTime(now)
      .build()

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
