package it.pagopa.interop.commons.queue

import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.regions.Region

case class QueueAccountInfo(region: Region, accessKeyId: String, secretAccessKey: String)

object QueueConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  val queueAccountInfo: QueueAccountInfo = QueueAccountInfo(
    region = Region.of(config.getString("interop-commons.queue.region")),
    accessKeyId = config.getString("interop-commons.queue.access-key-id"),
    secretAccessKey = config.getString("interop-commons.queue.secret-access-key")
  )
}
