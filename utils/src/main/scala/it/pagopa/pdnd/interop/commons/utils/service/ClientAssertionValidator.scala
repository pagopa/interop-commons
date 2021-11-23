package it.pagopa.pdnd.interop.commons.utils.service

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/** Validates client assertions
  */
trait ClientAssertionValidator {

  /** Validates a client assertion bore by consumer to check if it is valid for the corresponding client.
    *
    * @param clientAssertion client assertion as JWT
    * @param clientAssertionType type of client assertion bore by client. It must be of <code>jwt-bearer</code> type.
    * @param grantType type of client assertion flow, it must be <code>client_credentials</code>
    * @param clientId identifier of the client corresponding to the assertion
    * @param getPublicKeyByClientId function to retrieve the public key corresponding to the client id.
    *                               The first UUID parameter is the client id, the second String parameter is the expected kid.
    * @param ec implicit <code>ExecutionContext</code> for multithreading execution
    * @return a couple containing the clientId and the corresponding validated JWT assertion
    */
  def validate(clientAssertion: String, clientAssertionType: String, grantType: String, clientId: Option[UUID])(
    getPublicKeyByClientId: (UUID, String) => Future[String]
  )(implicit ec: ExecutionContext): Future[(String, Boolean)]

}
