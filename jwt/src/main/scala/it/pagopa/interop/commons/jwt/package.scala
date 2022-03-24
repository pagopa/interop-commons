package it.pagopa.interop.commons

import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK

import scala.util.Try

package object jwt {
  type KID           = String
  type SerializedKey = String

  final val clientIdClaim = "client_id"
  final val typClaim      = "typ"

  private[jwt] def rsaVerifier(jwkKey: String): Try[RSASSAVerifier] = {
    Try {
      val jwk: JWK  = JWK.parse(jwkKey)
      val publicKey = jwk.toRSAKey
      new RSASSAVerifier(publicKey)
    }
  }

  private[jwt] def ecVerifier(jwkKey: String): Try[ECDSAVerifier] =
    Try {
      val jwk: JWK  = JWK.parse(jwkKey)
      val publicKey = jwk.toECKey
      new ECDSAVerifier(publicKey)
    }
}
