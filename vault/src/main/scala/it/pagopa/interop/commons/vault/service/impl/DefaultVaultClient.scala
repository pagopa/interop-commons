package it.pagopa.interop.commons.vault.service.impl

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}
import it.pagopa.interop.commons.vault.VaultClientConfiguration
import it.pagopa.interop.commons.vault.service.VaultClientInstance

/** Defines configuration setup for a Vault client implementation
  */
object DefaultVaultClient {
  private lazy val configuration      = VaultClientConfiguration.vaultConfig
  private lazy val vaultClient: Vault = {
    val config = new VaultConfig()
      .address(configuration.address)
      .token(configuration.token)
      .sslConfig(new SslConfig().verify(configuration.sslEnabled).build())
      .build()
    new Vault(config)
  }

  /** Defines DI instance for a Vault client
    */
  trait DefaultClientInstance extends VaultClientInstance {
    val client: Vault = vaultClient
  }
}
