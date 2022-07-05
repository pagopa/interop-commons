package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.jwt.PrivateKeysKidHolder
import it.pagopa.interop.commons.jwt.model.SessionTokenSeed
import it.pagopa.interop.commons.jwt.service.SessionTokenGenerator
import it.pagopa.interop.commons.signer.errors.EmptySignatureAlgorithmError
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

/** Default implementation for the generation of consumer Interop tokens
  */
class DefaultSessionTokenGenerator(val signerService: SignerService, val kidHolder: PrivateKeysKidHolder)(implicit
  ec: ExecutionContext
) extends SessionTokenGenerator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Generates Interop token
    *
    * @param signatureAlgorithm        SignatureAlgorithm
    * @param claimsSet                 map containing the claims to add to the token
    * @param audience                  audience of the generated token
    * @param tokenIssuer               value to set to the <code>iss</code> claim
    * @param validityDurationInSeconds long value representing the token duration
    * @return generated serialized token
    */
  override def generate(
    signatureAlgorithm: SignatureAlgorithm,
    claimsSet: Map[String, AnyRef],
    audience: Set[String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Future[String] = {
    for {
      interopPrivateKeyKid <- kidHolder.getPrivateKeyKidBySignatureAlgorithm(signatureAlgorithm).toFuture
      algorithm            <- getAlgorithm(signatureAlgorithm)
      seed                 <- SessionTokenSeed
        .createWithKid(
          kid = interopPrivateKeyKid,
          audience = audience,
          algorithm = algorithm,
          tokenIssuer = tokenIssuer,
          validityDurationSeconds = validityDurationInSeconds,
          claimsSet = claimsSet
        )
        .toFuture
      interopJWT           <- serializedJWTFromSessionTokenSeed(seed).toFuture
      signature            <- signerService.signData(interopPrivateKeyKid, signatureAlgorithm)(interopJWT)
      signedInteropJWT = s"$interopJWT.$signature"
      _                = logger.debug("Session Token generated")
    } yield signedInteropJWT
  }

  private def serializedJWTFromSessionTokenSeed(seed: SessionTokenSeed): Try[String] = Try {
    val issuedAt: Date       = new Date(seed.issuedAt)
    val notBeforeTime: Date  = new Date(seed.notBefore)
    val expirationTime: Date = new Date(seed.expireAt)

    val header: JWSHeader = new JWSHeader.Builder(seed.algorithm)
      .customParam("use", "sig")
      .`type`(`at+jwt`)
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

    s"${header.toBase64URL}.${payload.toPayload.toBase64URL}"
  }

  private def getAlgorithm(signatureAlgorithm: SignatureAlgorithm): Future[JWSAlgorithm] = signatureAlgorithm match {
    case SignatureAlgorithm.RSAPssSha256   => Future.successful(JWSAlgorithm.PS256)
    case SignatureAlgorithm.RSAPssSha384   => Future.successful(JWSAlgorithm.PS384)
    case SignatureAlgorithm.RSAPssSha512   => Future.successful(JWSAlgorithm.PS512)
    case SignatureAlgorithm.RSAPkcs1Sha256 => Future.successful(JWSAlgorithm.RS256)
    case SignatureAlgorithm.RSAPkcs1Sha384 => Future.successful(JWSAlgorithm.RS384)
    case SignatureAlgorithm.RSAPkcs1Sha512 => Future.successful(JWSAlgorithm.RS512)
    case SignatureAlgorithm.ECSha256       => Future.successful(JWSAlgorithm.ES256)
    case SignatureAlgorithm.ECSha384       => Future.successful(JWSAlgorithm.ES384)
    case SignatureAlgorithm.ECSha512       => Future.successful(JWSAlgorithm.ES512)
    case SignatureAlgorithm.Empty          => Future.failed(EmptySignatureAlgorithmError)
  }
}
