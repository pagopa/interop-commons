package it.pagopa.interop.commons.utils.service

import java.util.UUID

trait UUIDSupplier {
  def get(): UUID
}
