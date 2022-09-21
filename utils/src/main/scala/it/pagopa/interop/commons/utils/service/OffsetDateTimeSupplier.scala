package it.pagopa.interop.commons.utils.service

import java.time.OffsetDateTime

trait OffsetDateTimeSupplier {
  def get(): OffsetDateTime
}
