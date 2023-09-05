package it.pagopa.interop.commons.utils

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{
  MissingBearer,
  MissingClaim,
  MissingSub,
  MissingUserId
}

import scala.concurrent.Future
import java.util.UUID
import scala.util.Try
import scala.util.Failure
import scala.util.Success

object AkkaUtils extends AkkaUtils
trait AkkaUtils {
  @inline def getShard(id: String, numberOfShards: Int): String = Math.abs(id.hashCode % numberOfShards).toString

  object Authenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = {
      credentials match {
        case Provided(identifier) => Some(Seq(BEARER -> identifier))
        case Missing              => None
      }
    }
  }

  object PassThroughAuthenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = Some(Seq.empty)
  }

  // * This is not particularly elegant but they avoid converting each time the whole Seq to a map to extract just a value
  @inline def fastGetOpt(contexts: Seq[(String, String)])(k: String): Option[String]                                =
    contexts.find(_._1 == k).map(_._2)
  @inline private def fastGet(contexts: Seq[(String, String)])(k: String, ex: Throwable): Either[Throwable, String] =
    fastGetOpt(contexts)(k).toRight(ex)

  @inline def toFastUUID(either: Either[Throwable, String]): Either[Throwable, UUID] = either.flatMap(s =>
    Try(UUID.fromString(s)) match { // * AFAIK this packing/unpacking is unavoidable
      case Failure(ex2)  => Left(ex2)
      case Success(uuid) => Right(uuid)
    }
  )

  // * This is not particularly elegant but avoids converting the whole Either to Try before converting to Future
  @inline private def toFastFutureUUID(either: Either[Throwable, String]): Future[UUID] = either match {
    case Left(ex) => Future.failed(ex)
    case Right(s) =>
      Try(UUID.fromString(s)) match { // * AFAIK this packing/unpacking is unavoidable
        case Failure(ex2)  => Future.failed(ex2)
        case Success(uuid) => Future.successful(uuid)
      }
  }

  @inline private def toFuture[T](either: Either[Throwable, T]): Future[T] =
    either.fold(Future.failed, Future.successful)

  def getBearer(contexts: Seq[(String, String)]): Either[Throwable, String] = fastGet(contexts)(BEARER, MissingBearer)
  def getFutureBearer(contexts: Seq[(String, String)]): Future[String]      = toFuture(getBearer(contexts))

  def getUid(contexts: Seq[(String, String)]): Either[Throwable, String]   = fastGet(contexts)(UID, MissingUserId)
  def getUidUUID(contexts: Seq[(String, String)]): Either[Throwable, UUID] = toFastUUID(getUid(contexts))
  def getUidFuture(contexts: Seq[(String, String)]): Future[String]        = toFuture(getUid(contexts))
  def getUidFutureUUID(contexts: Seq[(String, String)]): Future[UUID]      = toFastFutureUUID(getUid(contexts))

  def getSub(contexts: Seq[(String, String)]): Either[Throwable, String]   = fastGet(contexts)(SUB, MissingSub)
  def getSubUUID(contexts: Seq[(String, String)]): Either[Throwable, UUID] = toFastUUID(getSub(contexts))
  def getSubFuture(contexts: Seq[(String, String)]): Future[String]        = toFuture(getSub(contexts))
  def getSubFutureUUID(contexts: Seq[(String, String)]): Future[UUID]      = toFastFutureUUID(getSub(contexts))

  def getOrganizationId(contexts: Seq[(String, String)]): Either[Throwable, String]   =
    fastGet(contexts)(ORGANIZATION_ID_CLAIM, MissingClaim(ORGANIZATION_ID_CLAIM))
  def getOrganizationIdUUID(contexts: Seq[(String, String)]): Either[Throwable, UUID] = toFastUUID(
    getOrganizationId(contexts)
  )
  def getOrganizationIdFuture(contexts: Seq[(String, String)]): Future[String]   = toFuture(getOrganizationId(contexts))
  def getOrganizationIdFutureUUID(contexts: Seq[(String, String)]): Future[UUID] = toFastFutureUUID(
    getOrganizationId(contexts)
  )

  def getUserRoles(contexts: Seq[(String, String)]): Either[Throwable, String]      =
    fastGet(contexts)(USER_ROLES, MissingClaim(USER_ROLES))
  def getUserRolesFuture(contexts: Seq[(String, String)]): Future[String]           = toFuture(getUserRoles(contexts))
  def getUserRolesListFuture(contexts: Seq[(String, String)]): Future[List[String]] = toFuture(
    getUserRoles(contexts).map(_.split(',').toList)
  )

  def getExternalIdOrigin(contexts: Seq[(String, String)]): Either[Throwable, String] =
    fastGet(contexts)(ORGANIZATION_EXTERNAL_ID_ORIGIN, MissingClaim(ORGANIZATION_EXTERNAL_ID_ORIGIN))
  def getExternalIdValue(contexts: Seq[(String, String)]): Either[Throwable, String]  =
    fastGet(contexts)(ORGANIZATION_EXTERNAL_ID_VALUE, MissingClaim(ORGANIZATION_EXTERNAL_ID_VALUE))
  def getExternalIdOriginFuture(contexts: Seq[(String, String)]): Future[String]      =
    toFuture(getExternalIdOrigin(contexts))
  def getExternalIdValueFuture(contexts: Seq[(String, String)]): Future[String]       =
    toFuture(getExternalIdValue(contexts))

  def getPurposeId(contexts: Seq[(String, String)]): Either[Throwable, String]   =
    fastGet(contexts)(PURPOSE_ID_CLAIM, MissingClaim(PURPOSE_ID_CLAIM))
  def getPurposeIdUUID(contexts: Seq[(String, String)]): Either[Throwable, UUID] = toFastUUID(getPurposeId(contexts))
  def getPurposeIdFuture(contexts: Seq[(String, String)]): Future[String]        = toFuture(getPurposeId(contexts))
  def getPurposeIdFutureUUID(contexts: Seq[(String, String)]): Future[UUID] = toFastFutureUUID(getPurposeId(contexts))

  def getSelfcareId(contexts: Seq[(String, String)]): Either[Throwable, String]   =
    fastGet(contexts)(SELFCARE_ID_CLAIM, MissingClaim(SELFCARE_ID_CLAIM))
  def getSelfcareIdUUID(contexts: Seq[(String, String)]): Either[Throwable, UUID] = toFastUUID(getSelfcareId(contexts))
  def getSelfcareIdFuture(contexts: Seq[(String, String)]): Future[String]        = toFuture(getSelfcareId(contexts))
  def getSelfcareIdFutureUUID(contexts: Seq[(String, String)]): Future[UUID] = toFastFutureUUID(getSelfcareId(contexts))

  def getAcceptLanguage(contexts: Seq[(String, String)]): Either[Throwable, String] =
    fastGet(contexts)(ACCEPT_LANGUAGE, MissingClaim(ACCEPT_LANGUAGE))
  def getAcceptLanguageFuture(contexts: Seq[(String, String)]): Future[String] = toFuture(getAcceptLanguage(contexts))

  def getClaim(contexts: Seq[(String, String)], claimName: String): Either[Throwable, String] =
    fastGet(contexts)(claimName, MissingClaim(claimName))
  def getClaimFuture(contexts: Seq[(String, String)], claimName: String): Future[String]      =
    toFuture(getClaim(contexts, claimName))

}
