package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.errors.InvalidJWTSign

import scala.util.{Failure, Try}

trait PublicKeysHolder {

  val RSApublicKeys: Try[Map[String, String]]
  val ECPublicKeys: Try[Map[String, String]]

  def verify(jwt: SignedJWT): Try[SignedJWT] = {
    for {
      algorithm   <- Try(jwt.getHeader.getAlgorithm)
      kid         <- Try(jwt.getHeader.getKeyID)
      verifier    <- getPublicKeyVerifierByAlgorithm(algorithm, kid)
      verifiedJWT <- Either.cond(jwt.verify(verifier), jwt, InvalidJWTSign).toTry
    } yield verifiedJWT
  }

  def verifyWithVerifier(verifier: JWSVerifier, jwt: SignedJWT): Try[SignedJWT] = {
    for {
      verifiedJWT <- Either.cond(jwt.verify(verifier), jwt, InvalidJWTSign).toTry
    } yield verifiedJWT
  }

  private def getPublicKeyVerifierByAlgorithm(algorithm: JWSAlgorithm, kid: String): Try[JWSVerifier] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(RSApublicKeys.get(kid))
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256 => rsaVerifier(RSApublicKeys.get(kid))
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K =>
        ecVerifier(ECPublicKeys.get(kid))
      case JWSAlgorithm.EdDSA => ecVerifier(ECPublicKeys.get(kid))
    }
  }

  def getVerifier(algorithm: JWSAlgorithm, publicKey: String): Try[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsaVerifier(publicKey)
    case JWSAlgorithm.ES256                                           => ecVerifier(publicKey)
    case _                                                            => Failure(new RuntimeException("Invalid key algorithm"))
  }

  private def rsaVerifier(jwkKey: String): Try[RSASSAVerifier] =
    Try {
      val jwk: JWK  = JWK.parse(jwkKey)
      val publicKey = jwk.toRSAKey
      new RSASSAVerifier(publicKey)
    }

  private def ecVerifier(jwkKey: String): Try[ECDSAVerifier] =
    Try {
      val jwk: JWK  = JWK.parse(jwkKey)
      val publicKey = jwk.toECKey
      new ECDSAVerifier(publicKey)
    }

}
