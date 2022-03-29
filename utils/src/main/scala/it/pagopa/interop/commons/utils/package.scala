package it.pagopa.interop.commons

import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{MissingBearer, MissingHeader}

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
  val IP_ADDRESS: String                                   = "X-Forwarded-For"
  val INTEROP_PRODUCT_NAME: String                         = "interop"

  def extractHeaders(contexts: Seq[(String, String)]): Either[ComponentError, (String, String, Option[String])] = {
    val contextsMap = contexts.toMap
    for {
      bearerToken   <- contextsMap.get(BEARER).toRight(MissingBearer)
      correlationId <- contextsMap.get(CORRELATION_ID_HEADER).toRight(MissingHeader(CORRELATION_ID_HEADER))
      ip = contextsMap.get(IP_ADDRESS)
    } yield (bearerToken, correlationId, ip)
  }
}
