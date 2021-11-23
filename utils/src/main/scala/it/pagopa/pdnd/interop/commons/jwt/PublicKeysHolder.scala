package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.errors.InvalidJWTSignature

import scala.util.{Failure, Try}

/** Contains the public keys required for validating PDND clients token
  */
trait PublicKeysHolder {

  /** Public keyset for RSA signatures
    */
  val RSAPublicKeyset: Map[KID, SerializedKey]

  /** Public keyset for EC signatures
    */
  val ECPublicKeyset: Map[KID, SerializedKey]

  /** Verifies if the JWT signature is valid
    * @param jwt token to verify
    * @return <code>Successful(true)</code> if the signature is valid
    */
  final def verify(jwt: SignedJWT): Try[Boolean] = {
    for {
      algorithm   <- Try(jwt.getHeader.getAlgorithm)
      kid         <- Try(jwt.getHeader.getKeyID)
      verifier    <- getPublicKeyVerifierByAlgorithm(algorithm, kid)
      verifiedJWT <- isSigned(verifier, jwt)
    } yield verifiedJWT
  }

  /** Verifies if the JWT signature is valid for the specified verifier.
    *
    * @param verifier instance doing the signature verification
    * @param jwt token to verify
    * @return <code>Successful(true)</code> if the signature is valid
    */
  final def isSigned(verifier: JWSVerifier, jwt: SignedJWT): Try[Boolean] = {
    for {
      verifiedJWT <- Either.cond(jwt.verify(verifier), true, InvalidJWTSignature).toTry
    } yield verifiedJWT
  }

  /** Given an algorithm specification and a public key, it returns the corresponding verifier instance.
    * @param algorithm algorithm type to use
    * @param publicKey public key used for building a verifier on it
    * @return the corresponding verifier
    */
  final def getVerifier(algorithm: JWSAlgorithm, publicKey: String): Try[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(Option(publicKey))
    case JWSAlgorithm.ES256                                           => ecVerifier(Option(publicKey))
    case _                                                            => Failure(new RuntimeException("Invalid key algorithm"))
  }

  private def getPublicKeyVerifierByAlgorithm(algorithm: JWSAlgorithm, kid: String): Try[JWSVerifier] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(RSAPublicKeyset.get(kid))
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256 => rsaVerifier(RSAPublicKeyset.get(kid))
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K =>
        ecVerifier(ECPublicKeyset.get(kid))
      case JWSAlgorithm.EdDSA => ecVerifier(ECPublicKeyset.get(kid))
    }
  }

  private def rsaVerifier(jwkKey: Option[String]): Try[RSASSAVerifier] = {
    Try {
      val keyVal    = jwkKey.get
      val jwk: JWK  = JWK.parse(keyVal)
      val publicKey = jwk.toRSAKey
      new RSASSAVerifier(publicKey)
    }
  }

  private def ecVerifier(jwkKey: Option[String]): Try[ECDSAVerifier] =
    Try {
      val keyVal    = jwkKey.get
      val jwk: JWK  = JWK.parse(keyVal)
      val publicKey = jwk.toECKey
      new ECDSAVerifier(publicKey)
    }

}
