package it.pagopa.interop.commons.signer.service

import it.pagopa.interop.commons.signer.model.SignatureAlgorithm

import scala.concurrent.Future

trait SignerService {
  def signData(keyId: String, signatureAlgorithm: SignatureAlgorithm)(data: String): Future[String]
}
