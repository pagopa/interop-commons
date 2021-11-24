package it.pagopa.pdnd.interop.commons.jwt.service

import com.nimbusds.jwt.JWTClaimsSet

import scala.util.Try

/** Gets bearer token claims coming from the client
  */
trait JWTReader {

  /** Returns the claims contained in the JWT bore as <code>Authorization</code> HTTP header <code>bearer</code>.
    * @param bearer attribute representing the bore authorization header
    * @return claims contained in the bearer, if the JWT is valid
    */
  def getClaims(bearer: String): Try[JWTClaimsSet]
}
