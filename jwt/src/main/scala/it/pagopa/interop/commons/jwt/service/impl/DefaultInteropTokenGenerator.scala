package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.model.{Token, TokenSeed}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.{M2M_ROLES, PrivateKeysKidHolder}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

/** Default implementation for the generation of consumer Interop tokens
  */
class DefaultInteropTokenGenerator(val signerService: SignerService, val kidHolder: PrivateKeysKidHolder)(implicit
  ec: ExecutionContext
) extends InteropTokenGenerator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def generate(
    clientAssertion: String,
    audience: List[String],
    customClaims: Map[String, AnyRef],
    tokenIssuer: String,
    validityDurationInSeconds: Long,
    isM2M: Boolean
  ): Future[Token] =
    for {
      clientAssertionToken <- Try(SignedJWT.parse(clientAssertion)).toFuture
      interopPrivateKeyKid <- kidHolder
        .getPrivateKeyKidByAlgorithm(clientAssertionToken.getHeader.getAlgorithm)
        .toFuture
      customClaimsMap = if (isM2M) customClaims ++ M2M_ROLES else customClaims
      tokenSeed       = TokenSeed
        .createWithKid(
          clientAssertionToken.getHeader.getAlgorithm,
          clientAssertionToken.getJWTClaimsSet.getSubject,
          interopPrivateKeyKid,
          audience,
          customClaimsMap,
          tokenIssuer,
          validityDurationInSeconds
        )
      interopJWT <- jwtFromSeed(tokenSeed).toFuture
      serializedToken = s"${interopJWT.getHeader.toBase64URL}.${interopJWT.getJWTClaimsSet.toPayload.toBase64URL}"
      signatureAlgorithm <- kidHolder.getPrivateKeyKidSignatureAlgorithm(clientAssertionToken.getHeader.getAlgorithm)
      signature          <- signerService.signData(interopPrivateKeyKid, signatureAlgorithm)(serializedToken)
      signedInteropJWT = s"$serializedToken.$signature"
      _                = logger.debug("Token generated")
    } yield Token(
      serialized = signedInteropJWT,
      jti = interopJWT.getJWTClaimsSet.getJWTID,
      iat = interopJWT.getJWTClaimsSet.getIssueTime.getTime / 1000,
      exp = interopJWT.getJWTClaimsSet.getExpirationTime.getTime / 1000,
      nbf = interopJWT.getJWTClaimsSet.getNotBeforeTime.getTime / 1000,
      expIn = validityDurationInSeconds,
      alg = clientAssertionToken.getHeader.getAlgorithm.getName,
      kid = interopPrivateKeyKid,
      aud = audience,
      sub = clientAssertionToken.getJWTClaimsSet.getSubject,
      iss = tokenIssuer
    )

  override def generateInternalToken(
    subject: String,
    audience: List[String],
    tokenIssuer: String,
    secondsDuration: Long
  ): Future[Token] =
    for {
      algorithm            <- Future.successful(JWSAlgorithm.RS256)
      interopPrivateKeyKid <- kidHolder.getPrivateKeyKidByAlgorithm(algorithm).toFuture
      tokenSeed = TokenSeed
        .createInternalTokenWithKid(
          algorithm = algorithm,
          kid = interopPrivateKeyKid,
          subject = subject,
          audience = audience,
          tokenIssuer = tokenIssuer,
          validityDurationSeconds = secondsDuration
        )
      interopJWT <- jwtFromSeed(tokenSeed).toFuture
      serializedToken = s"${interopJWT.getHeader.toBase64URL}.${interopJWT.getJWTClaimsSet.toPayload.toBase64URL}"
      signatureAlgorithm <- kidHolder.getPrivateKeyKidSignatureAlgorithm(algorithm)
      signature          <- signerService.signData(interopPrivateKeyKid, signatureAlgorithm)(serializedToken)
      signedInteropJWT = s"$serializedToken.$signature"
      _                = logger.debug("Interop internal Token generated")
    } yield Token(
      serialized = signedInteropJWT,
      jti = interopJWT.getJWTClaimsSet.getJWTID,
      iat = interopJWT.getJWTClaimsSet.getIssueTime.getTime / 1000,
      exp = interopJWT.getJWTClaimsSet.getExpirationTime.getTime / 1000,
      nbf = interopJWT.getJWTClaimsSet.getNotBeforeTime.getTime / 1000,
      expIn = secondsDuration,
      alg = algorithm.getName,
      kid = interopPrivateKeyKid,
      aud = audience,
      sub = subject,
      iss = tokenIssuer
    )

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

    val payload = seed.customClaims
      .foldLeft(builder)((jwtBuilder, k) => jwtBuilder.claim(k._1, k._2))
      .jwtID(seed.id.toString)
      .issuer(seed.issuer)
      .audience(seed.audience.asJava)
      .subject(seed.subject)
      .issueTime(issuedAt)
      .notBeforeTime(notBeforeTime)
      .expirationTime(expirationTime)
      .build()

    new SignedJWT(header, payload)
  }

}
