package it.pagopa.interop.commons

import java.security.MessageDigest
import java.time.format.DateTimeFormatter

package object utils {
  private[utils] lazy val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private[utils] lazy val sha1: MessageDigest              = MessageDigest.getInstance("SHA-1")
  private[utils] lazy val md5: MessageDigest               = MessageDigest.getInstance("MD5")
  val SUB: String                                          = "sub"
  val BEARER: String                                       = "bearer"
  val UID: String                                          = "uid"
  val ORGANIZATION: String                                 = "organization"
  val CORRELATION_ID_HEADER: String                        = "X-Correlation-Id"
  val IP_ADDRESS: String                                   = "X-Forwarded-For"
  val ADMITTABLE_HEADERS                                   = Set(CORRELATION_ID_HEADER, IP_ADDRESS)

  val INTEROP_PRODUCT_NAME: String = "interop"

  /** Returns all the admittable headers that can be forwarded downstream, given the request contexts passed as argument.
    * @param contexts HTTP request contexts
    * @return the <code>Map</code> of all the admittable headers to be forwarded downstream
    */
  def admittableHeaders(contexts: Seq[(String, String)]): Map[String, String] =
    contexts.toMap.filter(k => ADMITTABLE_HEADERS.contains(k._1))
}
