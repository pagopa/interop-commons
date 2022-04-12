package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader, JWSSigner}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.PrivateKeysHolder
import it.pagopa.interop.commons.jwt.model.{EC, JWTAlgorithmType, RSA, SessionTokenSeed}
import it.pagopa.interop.commons.jwt.service.SessionTokenGenerator
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

/** Default implementation for the generation of consumer Interop tokens
  */
trait DefaultSessionTokenGenerator extends SessionTokenGenerator { privateKeysHolder: PrivateKeysHolder =>
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Generates Interop token
    *
    * @param jwtAlgorithmType          - Algorithm type, either [[it.pagopa.interop.commons.jwt.model.RSA]] or [[it.pagopa.interop.commons.jwt.model.EC]]
    * @param claimsSet                 map containing the claims to add to the token
    * @param audience                  audience of the generated token
    * @param tokenIssuer               value to set to the <code>iss</code> claim
    * @param validityDurationInSeconds long value representing the token duration
    * @return generated serialized token
    */
  override def generate(
    jwtAlgorithmType: JWTAlgorithmType,
    claimsSet: Map[String, AnyRef],
    audience: Set[String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Try[String] = {
    for {
      interopPrivateKey <- getPrivateKeyByAlgorithmType(jwtAlgorithmType)
      seed <- SessionTokenSeed.create(
        key = interopPrivateKey,
        audience = audience,
        algorithm = getAlgorithm(jwtAlgorithmType),
        tokenIssuer = tokenIssuer,
        validityDurationSeconds = validityDurationInSeconds,
        claimsSet = claimsSet
      )
      interopJWT       <- jwtFromSessionTokenSeed(seed)
      tokenSigner      <- getSigner(seed.algorithm, interopPrivateKey)
      signedInteropJWT <- signToken(interopJWT, tokenSigner)
      serializedToken  <- Try(signedInteropJWT.serialize())
      _ = logger.debug("Session Token generated")
    } yield serializedToken
  }

  private def jwtFromSessionTokenSeed(seed: SessionTokenSeed): Try[SignedJWT] = Try {
    val issuedAt: Date       = new Date(seed.issuedAt)
    val notBeforeTime: Date  = new Date(seed.notBefore)
    val expirationTime: Date = new Date(seed.expireAt)

    val header: JWSHeader = new JWSHeader.Builder(seed.algorithm)
      .customParam("use", "sig")
      .`type`(JOSEObjectType.JWT)
      .keyID(seed.kid)
      .build()

    val builder: JWTClaimsSet.Builder = new JWTClaimsSet.Builder()
      .jwtID(seed.id.toString)
      .issuer(seed.issuer)
      .audience(seed.audience.toList.asJava)
      .issueTime(issuedAt)
      .notBeforeTime(notBeforeTime)
      .expirationTime(expirationTime)

    val payload = seed.claimsSet
      .foldLeft(builder)((jwtBuilder, k) => jwtBuilder.claim(k._1, k._2))
      .build()

    new SignedJWT(header, payload)
  }

  private def signToken(jwt: SignedJWT, signer: JWSSigner): Try[SignedJWT] = Try {
    val _ = jwt.sign(signer)
    jwt
  }

  private def getAlgorithm(algorithmType: JWTAlgorithmType): JWSAlgorithm = algorithmType match {
    case RSA => JWSAlgorithm.RS256
    case EC  => JWSAlgorithm.ES256
  }

}
