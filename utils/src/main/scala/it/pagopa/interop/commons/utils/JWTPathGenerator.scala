package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.format.DateTimeFormatter
import java.util.UUID

object JWTPathGenerator {
  def generateJWTPathInfo(dateTimeSupplier: OffsetDateTimeSupplier): (String, String) = {
    val now               = dateTimeSupplier.get()
    val formattedDate     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val path              = s"token-details/$formattedDate"
    val filename          = s"${formattedDateTime}_${UUID.randomUUID()}.ndjson"
    (path, filename)
  }
}
