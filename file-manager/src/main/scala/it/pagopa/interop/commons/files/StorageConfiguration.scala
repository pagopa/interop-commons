package it.pagopa.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

/** Models configuration data for connecting to the external storage, if any.
  *
  * @param applicationId application identifier as defined on third party storage (e.g.: AWS CLIENT)
  * @param applicationSecret application password as defined on third party storage (e.g.: AWS PASSWORD)
  * @param endpoint third party storage location (e.g.: AWS S3 endpoint)
  */
case class StorageAccountInfo(applicationId: String, applicationSecret: String, endpoint: String)

/** Defines File manager configuration
  */
object StorageConfiguration {
  private lazy val config: Config =
    ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns the file manager name as defined in configuration file.
    */
  val runtimeFileManager: String = config.getString("interop-commons.storage.type")

  /** Returns storage account configuration data
    */
  lazy val storageAccountInfo: StorageAccountInfo = StorageAccountInfo(
    applicationId = config.getString("interop-commons.storage.application.id"),
    applicationSecret = config.getString("interop-commons.storage.application.secret"),
    endpoint = config.getString("interop-commons.storage.endpoint")
  )
}
