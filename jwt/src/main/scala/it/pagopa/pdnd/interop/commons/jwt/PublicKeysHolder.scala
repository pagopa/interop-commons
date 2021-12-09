package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.jwt.errors.{InvalidJWTSignature, PublicKeyNotFound}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.OptionOps

import scala.util.{Failure, Try}

/** Contains the public keys required for validating PDND clients token
  */
trait PublicKeysHolder {

  /** Public keyset for JWT signatures
    */
  val publicKeyset: Map[KID, SerializedKey]

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
    for {
      keyset <- JWTConfiguration.jwtReader.loadKeyset()
      newKey <- keyset.get(kid).toTry(PublicKeyNotFound(s"Public key not found for kid $kid"))
      _ = publicKeyset + (kid -> newKey)
    } yield newKey
  }
}
