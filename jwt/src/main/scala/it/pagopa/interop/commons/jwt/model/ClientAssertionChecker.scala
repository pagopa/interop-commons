package it.pagopa.interop.commons.jwt.model

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.interop.commons.jwt.errors.{InvalidJWTSignature, JWSSignerNotAvailable}
import it.pagopa.interop.commons.jwt.{ecVerifier, rsaVerifier}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.util.{Failure, Try}

final class ClientAssertionChecker private (
  val jwt: SignedJWT,
  val kid: String,
  val subject: UUID,
  val purposeId: Option[UUID]
) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def verify(publicKey: String): Try[Unit] =
    for {
      verifier <- getVerifier(jwt.getHeader.getAlgorithm, publicKey)
      _ = logger.debug("Verify client signature with specific verifier")
      _ <- Either.cond(jwt.verify(verifier), true, InvalidJWTSignature).toTry
      _ = logger.debug("Client signature verified")
    } yield ()

  private[this] def getVerifier(algorithm: JWSAlgorithm, publicKey: String): Try[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(publicKey)
    case JWSAlgorithm.ES256                                           => ecVerifier(publicKey)
    case _ => Failure(JWSSignerNotAvailable(s"Algorithm ${algorithm.getName} not supported"))
  }
}

object ClientAssertionChecker {

  private[jwt] def apply(jwt: SignedJWT, kid: String, subject: UUID, purposeId: Option[UUID]): ClientAssertionChecker =
    new ClientAssertionChecker(jwt, kid, subject, purposeId)
}
