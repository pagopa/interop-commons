package it.pagopa.interop.commons.signer

import com.typesafe.config.{Config, ConfigFactory}

import java.net.URI
import java.time.Duration

final case class VaultConfig(address: String, token: String, sslEnabled: Boolean, signatureRoute: String) {
  def encryptionEndpoint(keyId: String) = new URI(s"$address/$signatureRoute/$keyId").normalize().toString
}

/** Signer configuration singleton
  */
object SignerConfiguration {

  private val config: Config = ConfigFactory.load()

  val maxConcurrency: Int             = config.getInt("interop-commons.kms.max-concurrency")
  val maxAcquisitionTimeout: Duration = config.getDuration("interop-commons.kms.max-acquisition-timeout")

}
