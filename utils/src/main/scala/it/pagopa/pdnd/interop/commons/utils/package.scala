package it.pagopa.pdnd.interop.commons

import java.security.MessageDigest
import java.time.format.DateTimeFormatter

package object utils {
  private[utils] lazy val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private[utils] lazy val sha1: MessageDigest              = MessageDigest.getInstance("SHA-1")
  private[utils] lazy val md5: MessageDigest               = MessageDigest.getInstance("MD5")
  lazy val BEARER                                          = "bearer"
  lazy val UID                                             = "uid"
  lazy val CORRELATION_ID_HEADER                           = "X-Correlation-Id"
}
