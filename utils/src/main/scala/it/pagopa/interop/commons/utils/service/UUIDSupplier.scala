package it.pagopa.interop.commons.utils.service

import java.util.UUID

trait UUIDSupplier {
  def get(): UUID
}

object UUIDSupplier extends UUIDSupplier {
  override def get(): UUID = UUID.randomUUID()
}
