package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import it.pagopa.interop.commons.jwt.model.RSA
import it.pagopa.interop.commons.jwt.service.impl.DefaultSessionTokenGenerator
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util
import java.util.UUID
import scala.util.Success

class SessionTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val rsaKey               = new RSAKeyGenerator(2048).generate
  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString

  val generator = new DefaultSessionTokenGenerator with PrivateKeysHolder {
    val RSAPrivateKeyset = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toJSONString)
    val ECPrivateKeyset  = Map.empty
  }

  "a SessionTokenGenerator" should {

    "generate a valid Session Token" in {

      val uid: SerializedKey                           = UUID.randomUUID().toString
      val organizationClaims: util.Map[String, String] = util.Map.ofEntries(
        util.Map.entry("id", "id"),
        util.Map.entry("role", "role"),
        util.Map.entry("fiscalCode", "fiscalCode")
      )

      val issuerUUID                     = UUID.randomUUID().toString
      val claimsSet: Map[String, AnyRef] = Map("uid" -> uid, "organization" -> organizationClaims)

      val sessionToken = generator
        .generate(
          jwtAlgorithmType = RSA,
          claimsSet = claimsSet,
          audience = Set("test"),
          tokenIssuer = issuerUUID,
          validityDurationInSeconds = 4000L
        )

      sessionToken shouldBe a[Success[_]]

      val signed = SignedJWT.parse(sessionToken.get)

      signed.getJWTClaimsSet.getStringClaim("uid") shouldBe uid
      signed.getJWTClaimsSet
        .getClaim("organization")
        .asInstanceOf[util.Map[String, String]] shouldBe organizationClaims

      val verifier = new RSASSAVerifier(rsaKey.toRSAKey)
      signed.verify(verifier) shouldBe true
    }

  }

}
