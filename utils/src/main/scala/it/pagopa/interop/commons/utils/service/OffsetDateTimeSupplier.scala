package it.pagopa.interop.commons.utils.service

import java.time.OffsetDateTime

/** Supplies a timestamp through DI
  */
trait OffsetDateTimeSupplier {
  def get: OffsetDateTime
}
