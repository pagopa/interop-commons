package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.jwt.errors.{InvalidJWTSignature, PublicKeyNotFound}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.OptionOps

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

  /* Verifies if the JWT signature is valid
   * @param jwt token to verify
   * @return <code>Successful(true)</code> if the signature is valid
   */
  private[jwt] final def verify(jwt: SignedJWT): Try[Unit] = {
    for {
      algorithm <- Try(jwt.getHeader.getAlgorithm)
      kid       <- Try(jwt.getHeader.getKeyID)
      verifier  <- getPublicKeyVerifierByAlgorithm(algorithm, kid)
      _         <- Either.cond(jwt.verify(verifier), true, InvalidJWTSignature).toTry
    } yield ()
  }

  private[this] def getPublicKeyVerifierByAlgorithm(algorithm: JWSAlgorithm, kid: String): Try[JWSVerifier] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 | JWSAlgorithm.PS256 | JWSAlgorithm.PS384 |
          JWSAlgorithm.PS256 =>
        RSAPublicKeyset.get(kid).toTry(PublicKeyNotFound).flatMap(rsaVerifier)
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K | JWSAlgorithm.EdDSA =>
        ECPublicKeyset.get(kid).toTry(PublicKeyNotFound).flatMap(ecVerifier)
      case _ => Failure(PublicKeyNotFound("Algorithm not supported"))
    }
  }

}
