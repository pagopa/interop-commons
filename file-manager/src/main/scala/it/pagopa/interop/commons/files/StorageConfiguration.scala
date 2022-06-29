package it.pagopa.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String)

object StorageConfiguration {
  val maxConcurrency: Int = ConfigFactory.load().getInt("interop-commons.storage.max-concurrency")
}
