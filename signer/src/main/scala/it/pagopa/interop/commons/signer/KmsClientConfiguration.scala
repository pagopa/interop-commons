package it.pagopa.interop.commons.signer

import com.typesafe.config.{Config, ConfigFactory}

import java.net.URI

final case class KmsClientConfig(keyId: String, signatureAlgorithm: String)

/** KMS configuration singleton
  */
object KmsClientConfiguration {

  private lazy val hoconConfig: Config =
    ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns currently vault configuration data
    */
  val kmsConfig =
    KmsClientConfig(
      keyId = hoconConfig.getString("interop-commons.kms.keyId"),
      signatureAlgorithm = hoconConfig.getString("interop-commons.kms.signatureAlgorithm")
    )

}
