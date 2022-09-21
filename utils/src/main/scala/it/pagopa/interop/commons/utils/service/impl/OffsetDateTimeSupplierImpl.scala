package it.pagopa.interop.commons.utils.service.impl

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime

case object OffsetDateTimeSupplierImpl extends OffsetDateTimeSupplier {
  override def get(): OffsetDateTime = OffsetDateTime.now()
}
