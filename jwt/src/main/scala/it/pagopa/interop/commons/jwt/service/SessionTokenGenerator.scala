package it.pagopa.interop.commons.jwt.service

import it.pagopa.interop.commons.signer.model.SignatureAlgorithm

import scala.concurrent.Future

/** Generates JWT token for Interop consumers
  */
trait SessionTokenGenerator {

  /** Generates Interop token
 *
    * @param SignatureAlgorithm          - Algorithm type, either [[RSA]] or [[EC]]
    * @param claimsSet                 map containing the claims to add to the token
    * @param audience                  audience of the generated token
    * @param tokenIssuer               value to set to the <code>iss</code> claim
    * @param validityDurationInSeconds long value representing the token duration
    * @return generated serialized token
    */
  def generate(
    signatureAlgorithm: SignatureAlgorithm,
    claimsSet: Map[String, AnyRef],
    audience: Set[String],
    tokenIssuer: String,
    validityDurationInSeconds: Long
  ): Future[String]

}
