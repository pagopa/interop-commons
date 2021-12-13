package it.pagopa.pdnd.interop.commons.jwt.service

import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.getBearer

import scala.util.Try

/** Gets bearer token claims coming from the client
  */
trait JWTReader {

  /** Returns the claims contained in the JWT bore as <code>Authorization</code> HTTP header <code>bearer</code>.
    * @param bearer attribute representing the bore authorization header
    * @return claims contained in the bearer, if the JWT is valid
    */
  def getClaims(bearer: String): Try[JWTClaimsSet]

  /** Gets the bearer string from headers and checks if it contains a valid JWT.
    * @param contexts - HTTP headers contexts
    * @return the bearer token, if it contains a valid JWT
    */
  def parseValidBearer(contexts: Seq[(String, String)]): Try[String] =
    for {
      bearer <- getBearer(contexts)
      _      <- getClaims(bearer)
    } yield bearer
}
