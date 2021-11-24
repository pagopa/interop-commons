package it.pagopa.pdnd.interop.commons.vault.service.impl

import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.commons.vault.service.{VaultClientInstance, VaultService}

import scala.jdk.CollectionConverters.MapHasAsScala

trait DefaultVaultService extends VaultService { clientInstance: VaultClientInstance =>

  override def read(path: String): Map[String, String] = {
    val data: LogicalResponse = client.logical().read(path)
    data.getData.asScala.toMap.view.toMap
  }

}
