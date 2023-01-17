package it.pagopa.interop.commons.jwt.service.impl

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.jwt.PublicKeysHolder
import it.pagopa.interop.commons.jwt.errors.{InvalidJWTClaim, UnableToParseJWT}
import it.pagopa.interop.commons.jwt.service.JWTReader

import scala.util.{Failure, Try}

trait DefaultJWTReader extends JWTReader {

  publicKeysHolder: PublicKeysHolder =>

  protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext]

  override def getClaims(bearer: String): Try[JWTClaimsSet] = for {
    jwt <- parseJWT(bearer)
    _   <- verifyJWTClaims(jwt)
    _   <- publicKeysHolder.verify(jwt)
  } yield jwt.getJWTClaimsSet

  private def parseJWT(bearer: String): Try[SignedJWT] =
    Try(SignedJWT.parse(bearer)).recoverWith(ex => Failure(UnableToParseJWT(ex.getMessage)))

  private def verifyJWTClaims(jwt: SignedJWT): Try[Unit] =
    Try(claimsVerifier.verify(jwt.getJWTClaimsSet, null)).recoverWith(ex => Failure(InvalidJWTClaim(ex.getMessage)))

}
