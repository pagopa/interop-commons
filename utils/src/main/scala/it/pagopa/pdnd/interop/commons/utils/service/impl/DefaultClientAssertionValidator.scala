package it.pagopa.pdnd.interop.commons.utils.service.impl

import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.jwt.validations.ClientAssertionValidation
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.{StringOps, TryOps}
import it.pagopa.pdnd.interop.commons.utils.service.ClientAssertionValidator
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** Default implementation for PDND consumer's client assertion validations.
  */
trait DefaultClientAssertionValidator extends ClientAssertionValidator with ClientAssertionValidation {

  publicKeysHolder: PublicKeysHolder =>

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  )(
    getPublicKeyByClientId: (UUID, String) => Future[String]
  )(implicit ex: ExecutionContext): Future[(String, Boolean)] =
    for {
      info <- extractJwtInfo(clientAssertion, clientAssertionType, grantType, clientId)
      (jwt, kid, clientId) = info
      clientUUID <- clientId.toFutureUUID
      publicKey  <- getPublicKeyByClientId(clientUUID, kid)
      verifier   <- getVerifier(jwt.getHeader.getAlgorithm, publicKey).toFuture
      _ = logger.info("Verify client signature with specific verifier")
      verified <- isSigned(verifier, jwt).toFuture
      _ = logger.info("Client signature verified")
    } yield clientId -> verified

  private def extractJwtInfo(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Future[(SignedJWT, String, String)] =
    Future.fromTry {
      for {
        _       <- validateAccessTokenRequest(clientAssertionType, grantType)
        jwt     <- Try(SignedJWT.parse(clientAssertion))
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
    }

}
