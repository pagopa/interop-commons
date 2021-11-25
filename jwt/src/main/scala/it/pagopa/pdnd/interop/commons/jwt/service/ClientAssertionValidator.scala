package it.pagopa.pdnd.interop.commons.jwt.service

import it.pagopa.pdnd.interop.commons.jwt.{KID, SerializedKey}

import java.util.UUID
import scala.util.Try

/** Validates client assertions
  */
trait ClientAssertionValidator {

  /** Validates a client assertion bore by consumer to check if it is valid for the corresponding client.
    *
    * @param clientAssertion client assertion as JWT
    * @param clientAssertionType type of client assertion bore by client. It must be of <code>jwt-bearer</code> type.
    * @param grantType type of client assertion flow, it must be <code>client_credentials</code>
    * @param clientUUID identifier of the client corresponding to the assertion
    * @param clientKeys contains client public keys indexed by kid
    * @return <code>Success</code> if the Client Assertion is valid.
    */
  def validate(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientUUID: Option[UUID],
    clientKeys: Map[KID, SerializedKey]
  ): Try[Unit]

}
