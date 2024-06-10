package it.pagopa.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

object StorageConfiguration {
  val config: Config = ConfigFactory.load()

  val maxConcurrency: Int = config.getInt("interop-commons.storage.max-concurrency")
}
