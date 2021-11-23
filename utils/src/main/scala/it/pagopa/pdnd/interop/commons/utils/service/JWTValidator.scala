package it.pagopa.pdnd.interop.commons.utils.service

import com.nimbusds.jwt.JWTClaimsSet

import scala.concurrent.{ExecutionContext, Future}

/** Validates bearer tokens coming from the client
  */
trait JWTValidator {

  /** Validates the content of the <code>Authorization</code> HTTP header bore as <code>bearer</code>.
    * @param bearer attribute representing the bore authz header
    * @param ec implicit <code>ExecutionContext</code> for multithreading execution
    * @return the claims contained in the bearer
    */
  def validate(bearer: String)(implicit ec: ExecutionContext): Future[JWTClaimsSet]
}
