package it.pagopa.pdnd.interop.commons.vault.service.impl

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}
import it.pagopa.pdnd.interop.commons.vault.VaultClientConfiguration

/** Defines configuration setup for a Vault client implementation
  */
object VaultClientSetup {

  lazy val configuration = VaultClientConfiguration.vaultConfig

  lazy val vaultClient: Vault = {
    val config = new VaultConfig()
      .address(configuration.address)
      .token(configuration.token)
      .sslConfig(new SslConfig().verify(false).build())
      .build()
    new Vault(config)
  }

  /** Defines DI instance for a vault instance client
    */
  trait VaultClient {
    val client: Vault = vaultClient
  }
}
