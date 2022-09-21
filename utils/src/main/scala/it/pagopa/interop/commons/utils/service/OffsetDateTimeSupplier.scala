package it.pagopa.interop.commons.utils.service

import java.time.{OffsetDateTime, ZoneOffset}

trait OffsetDateTimeSupplier {
  def get(): OffsetDateTime
}

object OffsetDateTimeSupplier extends OffsetDateTimeSupplier {
  override def get(): OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
}
