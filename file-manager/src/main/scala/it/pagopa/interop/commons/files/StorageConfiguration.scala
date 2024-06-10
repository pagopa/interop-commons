package it.pagopa.interop.commons.files

import com.typesafe.config.ConfigFactory

object StorageConfiguration {
  val maxConcurrency: Int = ConfigFactory.load().getInt("interop-commons.storage.max-concurrency")
}
