package it.pagopa.pdnd.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String, container: String)

object StorageConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference())

  val runtimeFileManager: String = config.getString("pdnd-interop-commons.storage.type")

  val storageAccountInfo =
    StorageAccountInfo(
      applicationId = config.getString("pdnd-interop-commons.storage.application.id"),
      applicationSecret = config.getString("pdnd-interop-commons.storage.application.secret"),
      endpoint = config.getString("pdnd-interop-commons.storage.endpoint"),
      container = config.getString("pdnd-interop-commons.storage.container")
    )
}
