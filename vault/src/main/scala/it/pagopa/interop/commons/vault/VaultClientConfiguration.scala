package it.pagopa.interop.commons.vault

import com.typesafe.config.{Config, ConfigFactory}

import java.net.URI

final case class VaultConfig(address: String, token: String, sslEnabled: Boolean, signatureRoute: String) {
  def encryptionEndpoint(keyId: String) = new URI(s"$address/$signatureRoute/$keyId").normalize().toString
}

/** Vaults configuration singleton
  */
object VaultClientConfiguration {

  private lazy val hoconConfig: Config =
    ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns currently vault configuration data
    */
  val vaultConfig =
    VaultConfig(
      address = hoconConfig.getString("interop-commons.vault.address"),
      token = hoconConfig.getString("interop-commons.vault.token"),
      sslEnabled = hoconConfig.getBoolean("interop-commons.vault.sslEnabled"),
      signatureRoute = hoconConfig.getString("interop-commons.vault.signature-route")
    )

}
