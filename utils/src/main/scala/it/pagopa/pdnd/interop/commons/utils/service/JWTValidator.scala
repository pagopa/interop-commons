package it.pagopa.pdnd.interop.commons.utils.service

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait JWTValidator {

  def validateClientAssertion(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  )(getPublicKey: (UUID, String) => Future[String])(implicit ex: ExecutionContext): Future[(String, SignedJWT)]

  def validateBearer(bearer: String)(implicit ex: ExecutionContext): Future[JWTClaimsSet]
}
