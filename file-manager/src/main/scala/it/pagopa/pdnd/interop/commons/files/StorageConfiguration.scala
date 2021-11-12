package it.pagopa.pdnd.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

/** Models configuration data for connecting to the external storage, if any.
  *
  * @param applicationId application identifier as defined on third party storage (e.g.: AWS CLIENT)
  * @param applicationSecret application password as defined on third party storage (e.g.: AWS PASSWORD)
  * @param endpoint third party storage location (e.g.: AWS S3 endpoint)
  * @param container third party storage container (e.g.: AWS S3 bucket name)
  */
case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String, container: String)

/** Defines File manager configuration
  */
object StorageConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference())

  /** Returns the file manager name as defined in configuration file.
    */
  val runtimeFileManager: String = config.getString("pdnd-interop-commons.storage.type")

  /** Returns storage account configuration data
    */
  val storageAccountInfo =
    StorageAccountInfo(
      applicationId = config.getString("pdnd-interop-commons.storage.application.id"),
      applicationSecret = config.getString("pdnd-interop-commons.storage.application.secret"),
      endpoint = config.getString("pdnd-interop-commons.storage.endpoint"),
      container = config.getString("pdnd-interop-commons.storage.container")
    )
}
