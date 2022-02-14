package it.pagopa.pdnd.interop.commons.jwt.service.impl

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.pdnd.interop.commons.jwt.errors.{InvalidJWTSignature, JWSSignerNotAvailable, PublicKeyNotFound}
import it.pagopa.pdnd.interop.commons.jwt.service.ClientAssertionValidator
import it.pagopa.pdnd.interop.commons.jwt.model.{ClientAssertionRequest, ClientAssertionChecker}
import it.pagopa.pdnd.interop.commons.jwt.{ecVerifier, rsaVerifier}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.OptionOps
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.util.{Failure, Try}
import cats.implicits._

/** Default implementation for PDND consumer's client assertion validations.
  */
trait DefaultClientAssertionValidator extends ClientAssertionValidator {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext]

  def extractJwtInfo(clientAssertionRequest: ClientAssertionRequest): Try[ClientAssertionChecker] =
    for {
      jwt <- Try(SignedJWT.parse(clientAssertionRequest.clientAssertion))
      _   <- Try(claimsVerifier.verify(jwt.getJWTClaimsSet, null))
      clientIdString = clientAssertionRequest.clientId.map(_.toString)
      subject <- Try(jwt.getJWTClaimsSet.getSubject).ensureOr(subject =>
        new RuntimeException(s"ClientId ${clientIdString} not equal to subject $subject")
      )(subject => { subject == clientIdString.getOrElse(subject) })
      kid <- Try(jwt.getHeader.getKeyID)
    } yield ClientAssertionChecker(jwt, kid, subject)

}
