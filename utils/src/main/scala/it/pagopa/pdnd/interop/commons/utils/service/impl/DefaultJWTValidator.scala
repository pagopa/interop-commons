package it.pagopa.pdnd.interop.commons.utils.service.impl

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.pdnd.interop.commons.utils.service.JWTValidator
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** Default implementation for PDND clients JWT validations
  */
trait DefaultJWTValidator extends JWTValidator {

  publicKeysHolder: PublicKeysHolder =>

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(bearer: String)(implicit ex: ExecutionContext): Future[JWTClaimsSet] = {
    for {
      jwt <- Try(SignedJWT.parse(bearer)).toFuture
      _ = logger.info("Verify bearer")
      _ <- publicKeysHolder.verify(jwt).toFuture
      _ = logger.info("Bearer verified")
    } yield jwt.getJWTClaimsSet
  }

}
