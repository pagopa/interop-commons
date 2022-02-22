package it.pagopa.interop.commons.vault

import com.typesafe.config.{Config, ConfigFactory}

final case class VaultConfig(address: String, token: String, sslEnabled: Boolean)

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
      sslEnabled = hoconConfig.getBoolean("interop-commons.vault.sslEnabled")
    )

}
