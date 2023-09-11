package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.format.DateTimeFormatter
import java.util.UUID

object JWTPathGenerator {
  def createJWTPath(dateTimeSupplier: OffsetDateTimeSupplier): String = {
    val now               = dateTimeSupplier.get()
    val formattedDate     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    s"token-details/$formattedDate/${formattedDateTime}_${UUID.randomUUID()}.ndjson"
  }
}
