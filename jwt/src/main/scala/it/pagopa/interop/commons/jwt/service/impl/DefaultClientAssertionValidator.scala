package it.pagopa.interop.commons.jwt.service.impl

import cats.syntax.all._
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.errors.{
  InvalidPurposeIdFormat,
  InvalidSubject,
  InvalidSubjectFormat,
  KidNotFound,
  SubjectNotFound
}
import it.pagopa.interop.commons.jwt.model.{ClientAssertionChecker, ValidClientAssertionRequest}
import it.pagopa.interop.commons.jwt.service.ClientAssertionValidator
import it.pagopa.interop.commons.utils.{PURPOSE_ID_CLAIM, SUB}
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
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
      clientIdOpt = clientAssertionRequest.clientId
      _           = logger.debug("Getting subject claim")
      subject   <- subjectClaim(clientIdOpt, jwt.getJWTClaimsSet)
      purposeId <- purposeIdClaim(jwt.getJWTClaimsSet)
      kid       <- kidHeader(jwt)
    } yield ClientAssertionChecker(jwt, kid, subject, purposeId)

  private def subjectClaim(clientId: Option[UUID], claimSet: JWTClaimsSet): Try[UUID] =
    Try(Option(claimSet.getSubject)).flatMap(_.traverse(_.toUUID)) match {
      case Failure(_)                                     =>
        logger.warn("Subject with unexpected format in client assertion")
        Failure(InvalidSubjectFormat(Try(claimSet.getClaim(SUB).toString).getOrElse("")))
      case Failure(_) | Success(None)                     =>
        logger.warn("Subject not found in client assertion")
        Failure(SubjectNotFound)
      case Success(Some(s)) if s == clientId.getOrElse(s) => Success(s)
      case Success(Some(s))                               =>
        logger.warn(s"Subject value $s is not equal to the provided client_id $clientId")
        Failure(InvalidSubject(s.toString))
    }

  private def purposeIdClaim(claimSet: JWTClaimsSet): Try[Option[UUID]] =
    Try(Option(claimSet.getStringClaim(PURPOSE_ID_CLAIM)))
      .flatMap(_.traverse(_.toUUID))
      .adaptErr { case _ => InvalidPurposeIdFormat(Try(claimSet.getClaim(PURPOSE_ID_CLAIM).toString).getOrElse("")) }

  private def kidHeader(jwt: SignedJWT): Try[String] =
    Try(Option(jwt.getHeader.getKeyID)) match {
      case Failure(_) | Success(None) =>
        logger.error("Kid not found in client assertion")
        Failure(KidNotFound)
      case Success(Some(s))           => Success(s)
    }
}
