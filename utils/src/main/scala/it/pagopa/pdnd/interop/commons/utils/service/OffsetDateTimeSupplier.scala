package it.pagopa.pdnd.interop.commons.utils.service

import java.time.OffsetDateTime

trait OffsetDateTimeSupplier {
  def get: OffsetDateTime
}
