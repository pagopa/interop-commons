package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.jwk.JWKSet

import java.net.URL
import scala.util.Try
import scala.jdk.CollectionConverters.IterableHasAsScala

/** Models an entity for loading public keys from a remote well known endpoint
  * @param url – The JWK set URL. Must not be null.
  * @param connectTimeout – The URL connection timeout, in milliseconds. If zero no (infinite) timeout.
  * @param readTimeout – The URL read timeout, in milliseconds. If zero no (infinite) timeout.
  * @param sizeLimit – The read size limit, in bytes. If zero no limit.
  */
final case class JWTWellKnownReader(url: Try[URL], connectTimeout: Int, readTimeout: Int, sizeLimit: Int) {

  /** Queries the remote URI to retrieve a map of public keys indexed by their kid.
    * @return map of public keys
    */
  def loadKeyset(): Try[Map[KID, SerializedKey]] = {
    for {
      wellKnownURL <- url
      keyset       <- Try { JWKSet.load(wellKnownURL, connectTimeout, readTimeout, sizeLimit) }
      serializedKeys = keyset.getKeys.asScala.toList.map(f => (f.getKeyID, f.toJSONString)).toMap
    } yield serializedKeys
  }
}
