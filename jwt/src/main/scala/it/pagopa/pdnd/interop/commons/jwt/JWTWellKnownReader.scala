package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.jwk.JWKSet
import org.slf4j.{Logger, LoggerFactory}

import java.net.URL
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.Try

/** Models an entity for loading public keys from a remote well known endpoint
  * @param url The JWK set URL. Must not be null.
  * @param connectTimeout The URL connection timeout, in milliseconds. If zero no (infinite) timeout.
  * @param readTimeout The URL read timeout, in milliseconds. If zero no (infinite) timeout.
  * @param sizeLimit The read size limit, in bytes. If zero no limit.
  */
final case class JWTWellKnownReader(url: String, connectTimeout: Int, readTimeout: Int, sizeLimit: Int) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Queries the remote URI to retrieve a map of public keys indexed by their kid.
    * @return map of public keys
    */
  def loadKeyset(): Try[Map[KID, SerializedKey]] = {
    logger.debug("Getting key set from well-known url...")
    for {
      wellKnownURL <- Try { new URL(url) }
      keyset       <- Try { JWKSet.load(wellKnownURL, connectTimeout, readTimeout, sizeLimit) }
      _              = logger.debug("Public KeySet loaded")
      serializedKeys = keyset.getKeys.asScala.toList.map(f => (f.getKeyID, f.toJSONString)).toMap
      _              = logger.debug("Public KeySet serialized")
    } yield serializedKeys
  }
}
