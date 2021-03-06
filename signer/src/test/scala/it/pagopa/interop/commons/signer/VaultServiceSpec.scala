package it.pagopa.interop.commons.signer

import cats.implicits._
import com.bettercloud.vault.{Vault, VaultConfig}
import com.dimafeng.testcontainers.{ForAllTestContainer, VaultContainer}
import it.pagopa.interop.commons.signer.service.impl.DefaultVaultService
import it.pagopa.interop.commons.signer.service.{VaultClientInstance, VaultService}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.testcontainers.utility.DockerImageName

class VaultServiceSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  override val container: VaultContainer =
    new VaultContainer(
      dockerImageNameOverride = DockerImageName.parse("vault:1.9.0").some,
      vaultToken = Some("test-token")
    )

  container.vaultContainer.withSecretInVault(
    "secret/mock/path/vault",
    "secret_one=zeroLimestone",
    "secret_two=armadillosProphecy"
  )

  container.vaultContainer.withSecretInVault(
    "secret/mock/path/encoded",
    "encoded1=aGVsbG8gdGhlcmUsIG15IGZyaWVuZCE=",
    "encoded2=aG93IGFyZSB5b3U/"
  )

  // Dad won't be proud of this, but for tests it's acceptable, isn't it?
  var vaultService: VaultService = _

  override def beforeAll() = {
    // we need to instantiate this before all since we need that Docker container is running to get actual port
    vaultService = new DefaultVaultService with VaultClientInstance {
      override val client: Vault = {
        val config = new VaultConfig()
          .address(s"http://${container.vaultContainer.getHost}:${container.vaultContainer.getFirstMappedPort}")
          .token("test-token")
          .build()
        new Vault(config)
      }
    }
  }

  "VaultService" should {

    "read plain secrets properly" ignore {
      vaultService.read("secret/mock/path/vault") should contain only (
        "secret_one" -> "zeroLimestone",
        "secret_two" -> "armadillosProphecy",
      )
    }

    "read Base64 encoded secrets properly" ignore {
      vaultService.readBase64EncodedData("secret/mock/path/encoded") should contain only (
        "encoded1" -> "hello there, my friend!",
        "encoded2" -> "how are you?",
      )
    }
  }
}
