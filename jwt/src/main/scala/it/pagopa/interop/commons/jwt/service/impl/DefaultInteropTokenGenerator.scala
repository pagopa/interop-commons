package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader, JWSSigner}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.model.{JWTAlgorithmType, TokenSeed}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, PrivateKeysHolder}
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

/** Default implementation for the generation of consumer Interop tokens
  */
trait DefaultInteropTokenGenerator extends InteropTokenGenerator { privateKeysHolder: PrivateKeysHolder =>
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private lazy val jwtClaims = JWTConfiguration.jwtInternalTokenConfig

  override def generate(
    clientAssertion: String,
    subject: String,
    audience: List[String],
    customClaims: Map[String, String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Try[String] =
    for {
      clientAssertionToken <- Try(SignedJWT.parse(clientAssertion))
      interopPrivateKey    <- getPrivateKeyByAlgorithm(clientAssertionToken.getHeader.getAlgorithm)
      tokenSeed <- TokenSeed.create(
        clientAssertionToken,
        subject,
        interopPrivateKey,
        audience,
        customClaims,
        tokenIssuer,
        validityDurationInSeconds
      )
      interopJWT       <- jwtFromSeed(tokenSeed)
      tokenSigner      <- getSigner(tokenSeed.algorithm, interopPrivateKey)
      signedInteropJWT <- signToken(interopJWT, tokenSigner)
      serializedToken  <- Try(signedInteropJWT.serialize())
      _ = logger.debug("Token generated")
    } yield serializedToken

  override def generateInternalToken(
    jwtAlgorithmType: JWTAlgorithmType,
    subject: String,
    audience: List[String],
    tokenIssuer: String,
    secondsDuration: Long
  ): Try[String] =
    for {
      interopPrivateKey <- getPrivateKeyByAlgorithmType(jwtAlgorithmType)
      tokenSeed <- TokenSeed.createInternalToken(
        algorithm = JWSAlgorithm.RS256,
        key = interopPrivateKey,
        subject = subject,
        audience = audience,
        tokenIssuer = tokenIssuer,
        validityDurationSeconds = secondsDuration
      )
      interopJWT       <- jwtFromSeed(tokenSeed)
      tokenSigner      <- getSigner(tokenSeed.algorithm, interopPrivateKey)
      signedInteropJWT <- signToken(interopJWT, tokenSigner)
      serializedToken  <- Try(signedInteropJWT.serialize())
      _ = logger.debug("Interop internal Token generated")
    } yield serializedToken

  private def jwtFromSeed(seed: TokenSeed): Try[SignedJWT] = Try {
    val issuedAt: Date       = new Date(seed.issuedAt)
    val notBeforeTime: Date  = new Date(seed.nbf)
    val expirationTime: Date = new Date(seed.expireAt)

    val header: JWSHeader = new JWSHeader.Builder(seed.algorithm)
      .customParam("use", "sig")
      .`type`(`at+jwt`)
      .keyID(seed.kid)
      .build()

    val builder: JWTClaimsSet.Builder = new JWTClaimsSet.Builder()
      .jwtID(seed.id.toString)
      .issuer(seed.issuer)
      .audience(seed.audience.asJava)
      .subject(seed.subject)
      .issueTime(issuedAt)
      .notBeforeTime(notBeforeTime)
      .expirationTime(expirationTime)
    val payload = seed.customClaims
      .foldLeft(builder)((jwtBuilder, k) => jwtBuilder.claim(k._1, k._2))
      .build()

    new SignedJWT(header, payload)
  }

  private def signToken(jwt: SignedJWT, signer: JWSSigner): Try[SignedJWT] = Try {
    val _ = jwt.sign(signer)
    jwt
  }

}
