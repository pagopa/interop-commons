package it.pagopa.interop.commons.files

import com.typesafe.config.ConfigFactory

object StorageConfiguration {
  val maxConcurrency: Int        = ConfigFactory.load().getInt("interop-commons.storage.max-concurrency")
  val getUrlDurationMinutes: Int = ConfigFactory.load().getInt("get-url-duration-minutes")
  val putUrlDurationMinutes: Int = ConfigFactory.load().getInt("put-url-duration-minutes")
}
