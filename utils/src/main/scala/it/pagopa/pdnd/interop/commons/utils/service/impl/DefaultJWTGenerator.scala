package it.pagopa.pdnd.interop.commons.utils.service.impl

import com.nimbusds.jose.crypto.{ECDSASigner, Ed25519Signer, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader, JWSSigner}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.commons.jwt.PrivateKeysHolder
import it.pagopa.pdnd.interop.commons.jwt.model.TokenSeed
import it.pagopa.pdnd.interop.commons.utils.service.JWTGenerator
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

trait DefaultJWTGenerator extends JWTGenerator { privateKeysHolder: PrivateKeysHolder =>
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val purposesClaimName: String = "purposes"

  override def generatePDNDToken(
    clientAssertion: String,
    audience: List[String],
    purposes: String,
    tokenIssuer: String,
    validityDuration: Long
  ): Future[String] =
    Future.fromTry {
      for {
        jwt        <- Try(SignedJWT.parse(clientAssertion))
        privateKey <- privateKeysHolder.getPrivateKeyByAlgorithm(jwt.getHeader.getAlgorithm)
        seed       <- TokenSeed.create(jwt, privateKey, audience, purposes, tokenIssuer, validityDuration)
        token      <- createToken(seed)
        signer     <- getSigner(seed.algorithm, privateKey)
        signed     <- signToken(token, signer)
        _ = logger.info("Token generated")
      } yield toBase64(signed)
    }

  private def createToken(seed: TokenSeed): Try[SignedJWT] = Try {
    val issuedAt: Date       = new Date(seed.issuedAt)
    val notBeforeTime: Date  = new Date(seed.nbf)
    val expirationTime: Date = new Date(seed.expireAt)

    val header: JWSHeader = new JWSHeader.Builder(seed.algorithm)
      .customParam("use", "sig")
      .`type`(JOSEObjectType.JWT)
      .keyID(seed.kid)
      .build()

    val payload: JWTClaimsSet = new JWTClaimsSet.Builder()
      .issuer(seed.issuer)
      .audience(seed.audience.asJava)
      .subject(seed.clientId)
      .issueTime(issuedAt)
      .notBeforeTime(notBeforeTime)
      .expirationTime(expirationTime)
      .claim(purposesClaimName, seed.purposes)
      .build()

    new SignedJWT(header, payload)

  }

  def getSigner(algorithm: JWSAlgorithm, key: JWK): Try[JWSSigner] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => rsa(key)
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => rsa(key)
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ec(key)
      case JWSAlgorithm.EdDSA                                                                 => octet(key)

    }
  }

  private def rsa(jwk: JWK): Try[JWSSigner]   = Try(new RSASSASigner(jwk.toRSAKey))
  private def ec(jwk: JWK): Try[JWSSigner]    = Try(new ECDSASigner(jwk.toECKey))
  private def octet(jwk: JWK): Try[JWSSigner] = Try(new Ed25519Signer(jwk.toOctetKeyPair))
  private def signToken(jwt: SignedJWT, signer: JWSSigner): Try[SignedJWT] = Try {
    val _ = jwt.sign(signer)
    jwt
  }

  private def toBase64(jwt: SignedJWT): String = {
    s"""${jwt.getHeader.toBase64URL}.${jwt.getPayload.toBase64URL}.${jwt.getSignature}"""
  }

}
