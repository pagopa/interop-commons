package it.pagopa.pdnd.interop.commons.utils

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}

/** Mixes in Akka utilities
  */
trait AkkaUtils {
  @inline def getShard(id: String, numberOfShards: Int): String = Math.abs(id.hashCode % numberOfShards).toString

  /** Retrieves the bearer content from the HTTP request
    */
  object Authenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = {
      credentials match {
        case Provided(identifier) => Some(Seq("bearer" -> identifier))
        case Missing              => None
      }
    }
  }

  /** Bypasses any bearer content from the HTTP request
    */
  object PassThroughAuthenticator extends Authenticator[Seq[(String, String)]] {
    override def apply(credentials: Credentials): Option[Seq[(String, String)]] = Some(Seq.empty)
  }
}

/** Exposes Akka utilities
  */
object AkkaUtils extends AkkaUtils
