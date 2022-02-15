package it.pagopa.pdnd.interop.commons.jwt.service

import it.pagopa.pdnd.interop.commons.jwt.{KID, SerializedKey}
import it.pagopa.pdnd.interop.commons.jwt.model.{ValidClientAssertionRequest, ClientAssertionChecker}

import java.util.UUID
import scala.util.Try

/** Validates client assertions
  */
trait ClientAssertionValidator {

  /** Extracts a [[it.pagopa.pdnd.interop.commons.jwt.model.ClientAssertionChecker]] given a [[it.pagopa.pdnd.interop.commons.jwt.model.ClientAssertionRequest]]
    *
    * @param clientAssertionRequest payload containing client assertion
    * @return a [[it.pagopa.pdnd.interop.commons.jwt.model.ClientAssertionChecker]] if the provided jwt is valid
    */
  def extractJwtInfo(clientAssertionRequest: ValidClientAssertionRequest): Try[ClientAssertionChecker]

}
