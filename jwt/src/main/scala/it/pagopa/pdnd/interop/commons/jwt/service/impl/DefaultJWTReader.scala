package it.pagopa.pdnd.interop.commons.jwt.service.impl

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.commons.jwt.{JWTConfiguration, PublicKeysHolder}
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader

import scala.util.Try

/** Default implementation for PDND clients JWT reader
  */
trait DefaultJWTReader extends JWTReader {

  publicKeysHolder: PublicKeysHolder =>

  protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext]

  override def getClaims(bearer: String): Try[JWTClaimsSet] = {
    for {
      jwt <- Try(SignedJWT.parse(bearer))
      _   <- Try(claimsVerifier.verify(jwt.getJWTClaimsSet, null))
      _ = logger.debug("Verify bearer")
      _ <- publicKeysHolder.verify(jwt)
      _ = logger.debug("Bearer verified")
    } yield jwt.getJWTClaimsSet
  }

}
