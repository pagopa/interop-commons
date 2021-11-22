package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK

import scala.util.{Random, Try}

trait PrivateKeysHolder {

  val rsaPrivateKeys: Try[Map[String, String]]
  val ecPrivateKeys: Try[Map[String, String]]

  /*
  //   TODO: Start
//   TODO: this part is static and initialized at the start up
//   TODO - use a def instead of a val, but this approach generate to many calls to the vault
//   TODO - use a refreshing cache, more complex

  val rsaPrivateKey: Try[Map[String, String]] = {
    val path = VaultService.extractKeyPath("rsa", "private")
    path.map(vaultService.getSecret)
  }

  val ecPrivateKey: Try[Map[String, String]] = {
    val path = VaultService.extractKeyPath("ec", "private")
    path.map(vaultService.getSecret)
  }
  //  TODO:End
   */

  def getPrivateKeyByAlgorithm(algorithm: JWSAlgorithm): Try[JWK] = {
    val keys: Try[Map[String, String]] = algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => rsaPrivateKeys
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => rsaPrivateKeys
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ecPrivateKeys
      case JWSAlgorithm.EdDSA                                                                 => ecPrivateKeys

    }

    val randomKey: Try[(String, String)] = keys.flatMap(ks =>
      Random
        .shuffle(ks)
        .take(1)
        .headOption
        .toRight(new RuntimeException("PDND private key not found"))
        .toTry
    )

    randomKey.flatMap { case (k, v) =>
      readPrivateKeyFromString(v)
    }
  }

  private def readPrivateKeyFromString(keyString: String): Try[JWK] = Try {
    JWK.parse(keyString)
  }
}
