package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.jwt.PrivateKeysKidHolder
import it.pagopa.interop.commons.jwt.model.{EC, JWTAlgorithmType, RSA, SessionTokenSeed}
import it.pagopa.interop.commons.jwt.service.SessionTokenGenerator
import it.pagopa.interop.commons.utils.TypeConversions.{StringOps, TryOps}
import it.pagopa.interop.commons.vault.service.VaultTransitService
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
import scala.util.Try

/** Default implementation for the generation of consumer Interop tokens
  */
class DefaultSessionTokenGenerator(val vaultTransitService: VaultTransitService, val kidHolder: PrivateKeysKidHolder)(
  implicit ec: ExecutionContext
) extends SessionTokenGenerator {
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
  ): Future[String] = {
    for {
      interopPrivateKeyKid <- kidHolder.getPrivateKeyKidByAlgorithmType(jwtAlgorithmType).toFuture
      seed                 <- SessionTokenSeed
        .createWithKid(
          kid = interopPrivateKeyKid,
          audience = audience,
          algorithm = getAlgorithm(jwtAlgorithmType),
          tokenIssuer = tokenIssuer,
          validityDurationSeconds = validityDurationInSeconds,
          claimsSet = claimsSet
        )
        .toFuture
      interopJWT           <- jwtFromSessionTokenSeed(seed).toFuture
      encodedJWT           <- interopJWT.encodeBase64.toFuture
      signature            <- vaultTransitService.encryptData(interopPrivateKeyKid)(encodedJWT)
      signedInteropJWT = s"$interopJWT.$signature"
      _                = logger.debug("Session Token generated")
    } yield signedInteropJWT
  }

  private def jwtFromSessionTokenSeed(seed: SessionTokenSeed): Try[String] = Try {
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

  private def getAlgorithm(algorithmType: JWTAlgorithmType): JWSAlgorithm = algorithmType match {
    case RSA => JWSAlgorithm.RS256
    case EC  => JWSAlgorithm.ES256
  }

}
