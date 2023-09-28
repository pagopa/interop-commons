package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.format.DateTimeFormatter
import java.util.UUID

object JWTPathGenerator {
  case class JWTPathInfo(path: String, filename: String)

  def generateJWTPathInfo(dateTimeSupplier: OffsetDateTimeSupplier): JWTPathInfo = {
    val now               = dateTimeSupplier.get()
    val formattedDate     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    JWTPathInfo(s"token-details/$formattedDate", s"${formattedDateTime}_${UUID.randomUUID()}.ndjson")
  }
}
