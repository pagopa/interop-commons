package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.crypto.{ECDSASigner, Ed25519Signer, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSSigner}
import it.pagopa.interop.commons.jwt.errors.PrivateKeyNotFound
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm

import scala.util.{Random, Try}

trait PrivateKeysKidHolder {

  /** Private keyset for RSA signatures
    */
  val RSAPrivateKeyset: Set[KID]

  /** Private keyset for EC signatures
    */
  val ECPrivateKeyset: Set[KID]

  private[jwt] final def getPrivateKeyKidBySignatureAlgorithm(signatureAlgorithm: SignatureAlgorithm): Try[String] = {
    val keys: Try[Set[KID]] = Try {
      signatureAlgorithm match {
        case SignatureAlgorithm.RSAPssSha256   => RSAPrivateKeyset
        case SignatureAlgorithm.RSAPssSha384   => RSAPrivateKeyset
        case SignatureAlgorithm.RSAPssSha512   => RSAPrivateKeyset
        case SignatureAlgorithm.RSAPkcs1Sha256 => RSAPrivateKeyset
        case SignatureAlgorithm.RSAPkcs1Sha384 => RSAPrivateKeyset
        case SignatureAlgorithm.RSAPkcs1Sha512 => RSAPrivateKeyset
        case SignatureAlgorithm.ECSha256       => ECPrivateKeyset
        case SignatureAlgorithm.ECSha384       => ECPrivateKeyset
        case SignatureAlgorithm.ECSha512       => ECPrivateKeyset
        case SignatureAlgorithm.Empty          => Set.empty
      }
    }

    val randomKey: Try[String] = keys.flatMap(ks =>
      Random
        .shuffle(ks)
        .take(1)
        .headOption
        .toRight(PrivateKeyNotFound("Interop private key not found"))
        .toTry
    )

    randomKey
  }

  private[jwt] final def getPrivateKeyKidSignatureAlgorithm(algorithm: JWSAlgorithm): SignatureAlgorithm =
    algorithm match {
      case JWSAlgorithm.RS256 => SignatureAlgorithm.RSAPkcs1Sha256
      case JWSAlgorithm.RS384 => SignatureAlgorithm.RSAPkcs1Sha384
      case JWSAlgorithm.RS512 => SignatureAlgorithm.RSAPkcs1Sha512
      case JWSAlgorithm.PS256 => SignatureAlgorithm.RSAPssSha256
      case JWSAlgorithm.PS384 => SignatureAlgorithm.RSAPssSha384
      case JWSAlgorithm.PS512 => SignatureAlgorithm.RSAPssSha512
      case JWSAlgorithm.ES256 => SignatureAlgorithm.ECSha256
      case JWSAlgorithm.ES384 => SignatureAlgorithm.ECSha384
      case JWSAlgorithm.ES512 => SignatureAlgorithm.ECSha512
    }

  /* Returns a random private key kid picked from the available keyset according to the specified algorithm
   * @param algorithm JWS Algorithm type
   * @return JWK of the specific algorithm type
   */
  private[jwt] final def getPrivateKeyKidByAlgorithm(algorithm: JWSAlgorithm): Try[String] = {
    val keys: Try[Set[KID]] = Try {
      algorithm match {
        case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => RSAPrivateKeyset
        case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => RSAPrivateKeyset
        case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ECPrivateKeyset
        case JWSAlgorithm.EdDSA                                                                 => ECPrivateKeyset
      }
    }

    val randomKey: Try[String] = keys.flatMap(ks =>
      Random
        .shuffle(ks)
        .take(1)
        .headOption
        .toRight(PrivateKeyNotFound("Interop private key not found"))
        .toTry
    )

    randomKey
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
