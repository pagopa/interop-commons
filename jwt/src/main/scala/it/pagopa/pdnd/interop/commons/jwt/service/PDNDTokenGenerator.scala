package it.pagopa.pdnd.interop.commons.jwt.service

import it.pagopa.pdnd.interop.commons.jwt.model.JWTAlgorithmType

import scala.util.Try

/** Generates JWT token for PDND consumers
  */
trait PDNDTokenGenerator {

  /** Generates PDND token
    * @param clientAssertion client assertion used for token generation
    * @param audience audience of the generated token
    * @param customClaims map containing possible custom claims to add to the token
    * @param tokenIssuer value to set to the <code>iss</code> claim
    * @param validityDuration long value representing the token duration
    * @return generated serialized token
    */
  def generate(
    clientAssertion: String,
    audience: List[String],
    customClaims: Map[String, String],
    tokenIssuer: String,
    validityDuration: Long
  ): Try[String]

  /** Returns an internal JWT token signed with an RSA key.
    * <br/>
    * All the expected claims are retrieved from commons configuration parameters
    * @return
    */
  def generateInternalRSAToken(): Try[String]

  /** Returns an internal JWT token.
    * @param jwtAlgorithmType - Algorithm type, either [[it.pagopa.pdnd.interop.commons.jwt.model.RSA]] or [[it.pagopa.pdnd.interop.commons.jwt.model.EC]]
    * @param subject token <code>sub</code>
    * @param audience token <code>aud</code>
    * @param tokenIssuer token <code>iss</code>
    * @param millisecondsDuration token <code>exp</code>
    * @return
    */
  def generateInternalToken(
    jwtAlgorithmType: JWTAlgorithmType,
    subject: String,
    audience: List[String],
    tokenIssuer: String,
    millisecondsDuration: Long
  ): Try[String]
}
