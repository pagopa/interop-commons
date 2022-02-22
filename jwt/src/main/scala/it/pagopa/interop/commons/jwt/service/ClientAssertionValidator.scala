package it.pagopa.interop.commons.jwt.service

import it.pagopa.interop.commons.jwt.model.{ClientAssertionChecker, ValidClientAssertionRequest}

import scala.util.Try

/** Validates client assertions
  */
trait ClientAssertionValidator {

  /** Extracts a [[it.pagopa.interop.commons.jwt.model.ClientAssertionChecker]] given a [[it.pagopa.interop.commons.jwt.model.ClientAssertionRequest]]
    *
    * @param clientAssertionRequest payload containing client assertion
    * @return a [[it.pagopa.interop.commons.jwt.model.ClientAssertionChecker]] if the provided jwt is valid
    */
  def extractJwtInfo(clientAssertionRequest: ValidClientAssertionRequest): Try[ClientAssertionChecker]

}
