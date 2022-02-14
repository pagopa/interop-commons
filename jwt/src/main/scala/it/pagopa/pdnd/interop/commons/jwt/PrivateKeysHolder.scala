package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.crypto.{ECDSASigner, Ed25519Signer, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSSigner}
import it.pagopa.pdnd.interop.commons.jwt.errors.PrivateKeyNotFound
import it.pagopa.pdnd.interop.commons.jwt.model.{EC, JWTAlgorithmType, RSA}

import scala.util.{Random, Try}

trait PrivateKeysHolder {

  /** Private keyset for RSA signatures
    */
  val RSAPrivateKeyset: Map[KID, SerializedKey]

  /** Private keyset for RSA signatures
    */
  val ECPrivateKeyset: Map[KID, SerializedKey]

  private[jwt] final def getPrivateKeyByAlgorithmType(algorithmType: JWTAlgorithmType): Try[JWK] =
    algorithmType match {
      case RSA => getPrivateKeyByAlgorithm(JWSAlgorithm.RS256)
      case EC  => getPrivateKeyByAlgorithm(JWSAlgorithm.ES256)
    }

  /* Returns a random private key picked from the available keyset according to the specified algorithm
   * @param algorithm JWS Algorithm type
   * @return JWK of the specific algorithm type
   */
  private[jwt] final def getPrivateKeyByAlgorithm(algorithm: JWSAlgorithm): Try[JWK] = {
    val keys: Try[Map[KID, SerializedKey]] = Try {
      algorithm match {
        case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => RSAPrivateKeyset
        case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => RSAPrivateKeyset
        case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ECPrivateKeyset
        case JWSAlgorithm.EdDSA                                                                 => ECPrivateKeyset
      }
    }

    val randomKey: Try[(String, String)] = keys.flatMap(ks =>
      Random
        .shuffle(ks)
        .take(1)
        .headOption
        .toRight(PrivateKeyNotFound("PDND private key not found"))
        .toTry
    )

    randomKey.flatMap { case (k, v) =>
      Try { JWK.parse(v) }
    }
  }

  /* Returns a <code>JWSSigner</code> for the specified algorithm and key
   * @param algorithm the specified algorithm
   * @param key the JWK used for building the <code>JWSSigner</code> on.
   * @return JWSSigner for the specified key and algorithm
   */
  private[jwt] final def getSigner(algorithm: JWSAlgorithm, key: JWK): Try[JWSSigner] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => rsa(key)
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => rsa(key)
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ec(key)
      case JWSAlgorithm.EdDSA                                                                 => octet(key)
    }
  }

  private def rsa(jwk: JWK): Try[JWSSigner]   = Try(new RSASSASigner(jwk.toRSAKey))
  private def ec(jwk: JWK): Try[JWSSigner]    = Try(new ECDSASigner(jwk.toECKey))
  private def octet(jwk: JWK): Try[JWSSigner] = Try(new Ed25519Signer(jwk.toOctetKeyPair))

}
