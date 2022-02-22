package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.interop.commons.jwt.errors.{InvalidJWTSignature, PublicKeyNotFound}
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps

import scala.util.{Failure, Try}

/** Contains the public keys required for validating Interop clients token
  */
trait PublicKeysHolder {

  /** Public keyset for JWT signatures
    */
  /*

 _________  ________  ________  ________
|\___   ___\\   __  \|\   ___ \|\   __  \
\|___ \  \_\ \  \|\  \ \  \_|\ \ \  \|\  \
     \ \  \ \ \  \\\  \ \  \ \\ \ \  \\\  \
      \ \  \ \ \  \\\  \ \  \_\\ \ \  \\\  \
       \ \__\ \ \_______\ \_______\ \_______\
        \|__|  \|_______|\|_______|\|_______|

     TODO REMOVE THIS VAR!
   */
  private[jwt] var publicKeyset: Map[KID, SerializedKey]

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
        getPublicKey(kid).flatMap(rsaVerifier)
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K | JWSAlgorithm.EdDSA =>
        getPublicKey(kid).flatMap(ecVerifier)
      case _ => Failure(PublicKeyNotFound(s"Algorithm ${algorithm.getName} not supported"))
    }
  }

  private def getPublicKey(kid: String): Try[SerializedKey] = {
    val lookup = publicKeyset.get(kid).toTry(PublicKeyNotFound(s"Public key not found for kid $kid"))
    lookup.recoverWith { case PublicKeyNotFound(_) =>
      readFromWellKnown(kid)
    }
  }

  private def readFromWellKnown(kid: String): Try[SerializedKey] = {
    val serializedKey = for {
      keyset <- JWTConfiguration.jwtReader.loadKeyset()
      newKey <- keyset.get(kid).toTry(PublicKeyNotFound(s"Public key not found for kid $kid"))
    } yield newKey

    serializedKey.foreach(newKey => publicKeyset = publicKeyset + (kid -> newKey))
    serializedKey
  }
}
