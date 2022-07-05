package it.pagopa.interop.commons.signer.service.impl

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}
import it.pagopa.interop.commons.signer.service.VaultClientInstance
import it.pagopa.interop.commons.signer.VaultClientConfiguration

/** Defines configuration setup for a Vault client implementation
  */
object DefaultVaultClient {
  private lazy val configuration      = VaultClientConfiguration.vaultConfig
  private lazy val vaultClient: Vault = {
    val config = new VaultConfig()
      .engineVersion(2)
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
