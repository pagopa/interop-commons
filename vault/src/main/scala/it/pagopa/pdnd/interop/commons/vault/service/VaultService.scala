package it.pagopa.pdnd.interop.commons.vault.service

import com.bettercloud.vault.Vault

import java.util.Base64

/** Offers read only API towards Vault endpoints
  */
trait VaultService {

  /** Returns the key value store persisted at specified Vault path
    * @param path Vault path to lookup
    * @return corresponding key value store of the defined path
    */
  def read(path: String): Map[String, String]

  /** <strong>Returns the decoded version of Base64 encoded</strong> key value store persisted at specified path
    * @param path Vault path to lookup
    * @return corresponding key value store of the defined path <strong>properly decoded</strong>
    */
  def readBase64EncodedData(path: String): Map[String, String] = read(path).map { case (k, v) => k -> decodeBase64(v) }

  private def decodeBase64(encoded: String): String = {
    val decoded: Array[Byte] = Base64.getDecoder.decode(encoded)
    new String(decoded)
  }
}

/** Self type trait for VaultService client dependency
  */
trait VaultClientInstance {
  val client: Vault
}
