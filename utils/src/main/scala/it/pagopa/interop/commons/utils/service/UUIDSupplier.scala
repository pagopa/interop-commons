package it.pagopa.interop.commons.utils.service

import java.util.UUID

/** Models a UUID generator.
  */
trait UUIDSupplier {

  /** Returns a generated UUID.
    * @return generated random UUID.
    */
  def get(): UUID
}
