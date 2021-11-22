package it.pagopa.pdnd.interop.commons.utils.service

import scala.concurrent.Future

trait JWTGenerator {
  def generatePDNDToken(
    clientAssertion: String,
    audience: List[String],
    purposes: String,
    tokenIssuer: String,
    validityDuration: Long
  ): Future[String]
}
