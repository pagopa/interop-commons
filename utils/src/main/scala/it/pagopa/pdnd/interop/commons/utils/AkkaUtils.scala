package it.pagopa.pdnd.interop.commons.utils

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.pdnd.interop.commons.utils.errors.MissingBearer

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

  def getBearer(contexts: Seq[(String, String)]): Try[String] =
    contexts.toMap.get(BEARER).toRight(MissingBearer).toTry
  def getFutureBearer(contexts: Seq[(String, String)]): Future[String] = getBearer(contexts).toFuture

}

/** Exposes Akka utilities
  */
object AkkaUtils extends AkkaUtils
