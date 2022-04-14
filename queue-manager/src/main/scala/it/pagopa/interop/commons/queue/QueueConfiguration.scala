package it.pagopa.interop.commons.queue

import com.typesafe.config.{Config, ConfigFactory}

case class QueueAccountInfo(queueUrl: String, accessKeyId: String, secretAccessKey: String)

object QueueConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  val queueAccountInfo: QueueAccountInfo = QueueAccountInfo(
    queueUrl = config.getString("interop-commons.queue.url"),
    accessKeyId = config.getString("interop-commons.queue.access-key-id"),
    secretAccessKey = config.getString("interop-commons.queue.secret-access-key")
  )
}
