package it.pagopa.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String)

object StorageConfiguration {
  private lazy val config: Config =
    ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  lazy val maxConcurrency: Int = Option(config.getInt("interop-commons.storage.max-concurrency")).getOrElse(50)

  lazy val storageAccountInfo: StorageAccountInfo = StorageAccountInfo(
    applicationId = config.getString("interop-commons.storage.application.id"),
    applicationSecret = config.getString("interop-commons.storage.application.secret"),
    endpoint = config.getString("interop-commons.storage.endpoint")
  )
}
