package it.pagopa.pdnd.interop.commons.jwt.service.impl

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/** Default implementation for PDND clients JWT reader
  */
trait DefaultJWTReader extends JWTReader {

  publicKeysHolder: PublicKeysHolder =>

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getClaims(bearer: String): Try[JWTClaimsSet] = {
    for {
      jwt <- Try(SignedJWT.parse(bearer))
      _ = logger.info("Verify bearer")
      _ <- publicKeysHolder.verify(jwt)
      _ = logger.info("Bearer verified")
    } yield jwt.getJWTClaimsSet
  }

}
