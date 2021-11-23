package it.pagopa.pdnd.interop.commons.utils.service

import scala.concurrent.Future

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
  ): Future[String]
}
