package it.pagopa.interop.commons.jwt.service

import it.pagopa.interop.commons.jwt.model.Token

import scala.concurrent.Future

/** Generates JWT token for Interop consumers
  */
trait InteropTokenGenerator {

  /** Generates Interop token
    * @param clientAssertion client assertion used for token generation
    * @param audience audience of the generated token
    * @param customClaims map containing possible custom claims to add to the token
    * @param tokenIssuer value to set to the <code>iss</code> claim
    * @param validityDurationInSeconds long value representing the token duration
    * @return generated serialized token
    */
  def generate(
    clientAssertion: String,
    audience: List[String],
    customClaims: Map[String, String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Future[Token]

  /** Returns an internal JWT token.
    * @param subject token <code>sub</code>
    * @param audience token <code>aud</code>
    * @param tokenIssuer token <code>iss</code>
    * @param secondsDuration token <code>exp</code>
    * @return
    */
  def generateInternalToken(
    subject: String,
    audience: List[String],
    tokenIssuer: String,
    secondsDuration: Long
  ): Future[Token]
}
