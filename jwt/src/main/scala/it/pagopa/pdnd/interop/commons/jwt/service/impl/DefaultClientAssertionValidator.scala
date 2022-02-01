package it.pagopa.pdnd.interop.commons.jwt.service.impl

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.pdnd.interop.commons.jwt.errors.{InvalidJWTSignature, JWSSignerNotAvailable, PublicKeyNotFound}
import it.pagopa.pdnd.interop.commons.jwt.service.ClientAssertionValidator
import it.pagopa.pdnd.interop.commons.jwt.validations.ClientAssertionValidation
import it.pagopa.pdnd.interop.commons.jwt.{ecVerifier, rsaVerifier}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.OptionOps
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.util.{Failure, Try}

/** Default implementation for PDND consumer's client assertion validations.
  */
trait DefaultClientAssertionValidator extends ClientAssertionValidator with ClientAssertionValidation {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext]

  override def validate(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientUUID: Option[UUID],
    clientKeys: Map[String, String]
  ): Try[Unit] =
    for {
      (jwt, kid, clientId) <- extractJwtInfo(clientAssertion, clientAssertionType, grantType, clientUUID)
      publicKey            <- clientKeys.get(kid).toTry(PublicKeyNotFound(s"Client $clientId public key not found for kid $kid"))
      verifier             <- getVerifier(jwt.getHeader.getAlgorithm, publicKey)
      _ = logger.debug("Verify client signature with specific verifier")
      _ <- Either.cond(jwt.verify(verifier), true, InvalidJWTSignature).toTry
      _ = logger.debug("Client signature verified")
    } yield ()

  private def extractJwtInfo(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Try[(SignedJWT, String, String)] =
    for {
      jwt     <- Try(SignedJWT.parse(clientAssertion))
      _       <- Try(claimsVerifier.verify(jwt.getJWTClaimsSet, null))
      _       <- validateAccessTokenRequest(clientAssertionType, grantType)
      subject <- Try(jwt.getJWTClaimsSet.getSubject)
      clientId <- Either
        .cond(
          subject == clientId.map(_.toString).getOrElse(subject),
          subject,
          new RuntimeException(s"ClientId ${clientId.toString} not equal to subject $subject")
        )
        .toTry
      kid <- Try(jwt.getHeader.getKeyID)
    } yield (jwt, kid, clientId)

  /* Given an algorithm specification and a public key, it returns the corresponding verifier instance.
   * @param algorithm algorithm type to use
   * @param publicKey public key used for building a verifier on it
   * @return the corresponding verifier
   */
  private[this] def getVerifier(algorithm: JWSAlgorithm, publicKey: String): Try[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(publicKey)
    case JWSAlgorithm.ES256                                           => ecVerifier(publicKey)
    case _                                                            => Failure(JWSSignerNotAvailable(s"Algorithm ${algorithm.getName} not supported"))
  }
}
