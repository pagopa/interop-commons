package it.pagopa.interop.commons.vault.service

import scala.concurrent.Future

trait VaultTransitService {
  def encryptData(keyId: String, signatureAlgorithm: Option[String] = None)(data: String): Future[String]
}
