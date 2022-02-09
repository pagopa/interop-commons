package it.pagopa.pdnd.interop.commons

import java.security.MessageDigest
import java.time.format.DateTimeFormatter

package object utils {
  private[utils] lazy val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private[utils] lazy val sha1: MessageDigest              = MessageDigest.getInstance("SHA-1")
  private[utils] lazy val md5: MessageDigest               = MessageDigest.getInstance("MD5")
  val SUB: String                                          = "sub"
  val BEARER: String                                       = "bearer"
  val UID: String                                          = "uid"
  val CORRELATION_ID_HEADER: String                        = "X-Correlation-Id"

  val INTEROP_PRODUCT_NAME: String = "interop"
}
