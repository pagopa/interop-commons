package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.errors.{InvalidSubject, SubjectNotFound}
import it.pagopa.interop.commons.jwt.model.{ClientAssertionChecker, ValidClientAssertionRequest}
import it.pagopa.interop.commons.jwt.service.ClientAssertionValidator
import it.pagopa.interop.commons.utils.PURPOSE_ID_CLAIM
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/** Default implementation for Interop consumer's client assertion validations.
  */
trait DefaultClientAssertionValidator extends ClientAssertionValidator {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext]

  def extractJwtInfo(clientAssertionRequest: ValidClientAssertionRequest): Try[ClientAssertionChecker] =
    for {
      jwt <- Try(SignedJWT.parse(clientAssertionRequest.clientAssertion))
      _   <- Try(claimsVerifier.verify(jwt.getJWTClaimsSet, null))
      clientIdOpt = clientAssertionRequest.clientId.map(_.toString)
      _           = logger.debug("Getting subject claim")
      subject   <- subjectClaim(clientIdOpt, jwt.getJWTClaimsSet)
      purposeId <- Try(Option(jwt.getJWTClaimsSet.getStringClaim(PURPOSE_ID_CLAIM)))
      kid       <- Try(jwt.getHeader.getKeyID)
    } yield ClientAssertionChecker(jwt, kid, subject, purposeId)

  private def subjectClaim(clientId: Option[String], claimSet: JWTClaimsSet): Try[String] = {
    Try(claimSet.getSubject) match {
      case Failure(_) =>
        logger.error("Subject not found in this claim")
        Failure(SubjectNotFound)

      case Success(s) if s == clientId.getOrElse(s) => Success(s)
      case Success(s)                               =>
        logger.error(s"Subject value $s is not equal to the provided client_id $clientId")
        Failure(InvalidSubject(s))
    }
  }

}
