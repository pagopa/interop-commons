package it.pagopa.interop.commons.vault.service

import scala.concurrent.Future

trait VaultTransitService {
  def encryptData(keyId: String)(data: String): Future[String]
}
