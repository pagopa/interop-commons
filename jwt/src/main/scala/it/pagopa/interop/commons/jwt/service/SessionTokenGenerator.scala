package it.pagopa.interop.commons.jwt.service

import it.pagopa.interop.commons.jwt.model.JWTAlgorithmType

import scala.concurrent.Future

/** Generates JWT token for Interop consumers
  */
trait SessionTokenGenerator {

  /** Generates Interop token
    * @param jwtAlgorithmType - Algorithm type, either [[it.pagopa.interop.commons.jwt.model.RSA]] or [[it.pagopa.interop.commons.jwt.model.EC]]
    * @param claimsSet map containing the claims to add to the token
    * @param audience audience of the generated token
    * @param tokenIssuer value to set to the <code>iss</code> claim
    * @param validityDurationInSeconds long value representing the token duration
    * @return generated serialized token
    */
  def generate(
    jwtAlgorithmType: JWTAlgorithmType,
    claimsSet: Map[String, AnyRef],
    audience: Set[String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Future[String]

}
