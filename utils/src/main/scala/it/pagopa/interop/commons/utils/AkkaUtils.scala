package it.pagopa.interop.commons.utils

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{
  MissingBearer,
  MissingClaim,
  MissingSub,
  MissingUserId
}

import scala.concurrent.Future
import scala.util.Try

/** Mixes in Akka utilities
  */
trait AkkaUtils {
  @inline def getShard(id: String, numberOfShards: Int): String = Math.abs(id.hashCode % numberOfShards).toString

  /** Retrieves the bearer content from the HTTP request
    */
  object Authenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = {
      credentials match {
        case Provided(identifier) => Some(Seq(BEARER -> identifier))
        case Missing              => None
      }
    }
  }

  /** Bypasses any bearer content from the HTTP request
    */
  object PassThroughAuthenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = Some(Seq.empty)
  }

  def getBearer(contexts: Seq[(String, String)]): Try[String]          =
    contexts.toMap.get(BEARER).toRight(MissingBearer).toTry
  def getFutureBearer(contexts: Seq[(String, String)]): Future[String] = getBearer(contexts).toFuture

  def getUid(contexts: Seq[(String, String)]): Try[String] = contexts.toMap.get(UID).toRight(MissingUserId).toTry
  def getUidFuture(contexts: Seq[(String, String)]): Future[String] = getUid(contexts).toFuture

  def getSub(contexts: Seq[(String, String)]): Try[String]          = contexts.toMap.get(SUB).toRight(MissingSub).toTry
  def getSubFuture(contexts: Seq[(String, String)]): Future[String] = getSub(contexts).toFuture

  def getClaim(contexts: Seq[(String, String)], claimName: String): Try[String]          =
    contexts.toMap.get(claimName).toRight(MissingClaim(claimName)).toTry
  def getClaimFuture(contexts: Seq[(String, String)], claimName: String): Future[String] =
    getClaim(contexts, claimName).toFuture

}

/** Exposes Akka utilities
  */
object AkkaUtils extends AkkaUtils
