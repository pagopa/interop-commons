package it.pagopa.pdnd.interop.commons.utils.service.impl

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.jwt.validations.JWTValidation
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.{StringOps, TryOps}
import it.pagopa.pdnd.interop.commons.utils.service.JWTValidator
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait DefaultJWTValidator extends JWTValidator with JWTValidation {

  publicKeysHolder: PublicKeysHolder =>

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validateClientAssertion(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  )(getPublicKey: (UUID, String) => Future[String])(implicit ex: ExecutionContext): Future[(String, SignedJWT)] =
    for {
      info <- extractJwtInfo(clientAssertion, clientAssertionType, grantType, clientId)
      (jwt, kid, clientId) = info
      clientUUID <- clientId.toFutureUUID
      publicKey  <- getPublicKey(clientUUID, kid)
      verifier   <- publicKeysHolder.getVerifier(jwt.getHeader.getAlgorithm, publicKey).toFuture
      _ = logger.info("Verify client signature with specific verifier")
      verified <- publicKeysHolder.verifyWithVerifier(verifier, jwt).toFuture
      _ = logger.info("Client signature verified")
    } yield clientId -> verified

  override def validateBearer(bearer: String)(implicit ex: ExecutionContext): Future[JWTClaimsSet] = {
    for {
      jwt <- Try(SignedJWT.parse(bearer)).toFuture
      _ = logger.info("Verify bearer")
      _ <- publicKeysHolder.verify(jwt).toFuture
      _ = logger.info("Bearer verified")
    } yield jwt.getJWTClaimsSet
  }

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
