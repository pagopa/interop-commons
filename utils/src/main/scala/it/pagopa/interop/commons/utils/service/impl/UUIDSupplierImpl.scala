package it.pagopa.interop.commons.utils.service.impl

import it.pagopa.interop.commons.utils.service.UUIDSupplier

import java.util.UUID

class UUIDSupplierImpl extends UUIDSupplier {
  override def get(): UUID = UUID.randomUUID()
}
