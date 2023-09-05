package it.pagopa.interop.commons

import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{MissingBearer, MissingHeader}
import akka.http.scaladsl.model.headers.Language

import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import java.time.ZoneId

package object utils {
  private[utils] lazy val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private[utils] lazy val sha1: MessageDigest              = MessageDigest.getInstance("SHA-1")
  private[utils] lazy val md5: MessageDigest               = MessageDigest.getInstance("MD5")
  private[utils] val europeRome: ZoneId                    = ZoneId.of("Europe/Rome")
  val SUB: String                                          = "sub"
  val BEARER: String                                       = "bearer"
  val UID: String                                          = "uid"
  val ORGANIZATION: String                                 = "organization"
  val USER_ROLES: String                                   = "user-roles"
  val CORRELATION_ID_HEADER: String                        = "X-Correlation-Id"
  val IP_ADDRESS: String                                   = "X-Forwarded-For"
  val ACCEPT_LANGUAGE: String                              = "Accept-Language"
  val CONTENT_LANGUAGE: String                             = "Content-Language"
  val INTEROP_PRODUCT_NAME: String                         = "prod-interop"
  val PURPOSE_ID_CLAIM: String                             = "purposeId"
  val DIGEST_CLAIM: String                                 = "digest"
  val ORGANIZATION_ID_CLAIM: String                        = "organizationId"
  val SELFCARE_ID_CLAIM: String                            = "selfcareId"
  val ORGANIZATION_EXTERNAL_ID_CLAIM: String               = "externalId"
  val ORGANIZATION_EXTERNAL_ID_ORIGIN_CLAIM: String        = "origin"
  val ORGANIZATION_EXTERNAL_ID_VALUE_CLAIM: String         = "value"
  val ORGANIZATION_EXTERNAL_ID_ORIGIN: String              = "organizationExternalIdOrigin"
  val ORGANIZATION_EXTERNAL_ID_VALUE: String               = "organizationExternalIdValue"

  val OTHER_LANGUAGES: Seq[Language] = Seq(Language("en-EN"))
  val DEFAULT_LANGUAGE: Language     = Language("it-IT")

  type BearerToken   = String
  type CorrelationId = String
  type IpAddress     = String

  def extractHeaders(
    contexts: Seq[(String, String)]
  ): Either[ComponentError, (BearerToken, CorrelationId, Option[IpAddress])] = {
    val contextsMap = contexts.toMap
    for {
      bearerToken   <- contextsMap.get(BEARER).toRight(MissingBearer)
      correlationId <- contextsMap.get(CORRELATION_ID_HEADER).toRight(MissingHeader(CORRELATION_ID_HEADER))
      ip = contextsMap.get(IP_ADDRESS)
    } yield (bearerToken, correlationId, ip)
  }

  def withHeaders[T](
    f: (BearerToken, CorrelationId, Option[IpAddress]) => Future[T]
  )(implicit contexts: Seq[(String, String)]): Future[T] = extractHeaders(contexts) match {
    case Left(ex) => Future.failed(ex)
    case Right(x) => f.tupled(x)
  }

  def withUid[T](f: String => Future[T])(implicit contexts: Seq[(String, String)]): Future[T] =
    AkkaUtils.getUid(contexts) match {
      case Left(ex) => Future.failed(ex)
      case Right(x) => f(x)
    }
}
