package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.jwt.service.impl.DefaultPDNDTokenGenerator
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.Success

class PDNDTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val rsaKey               = new RSAKeyGenerator(2048).generate
  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString

  val generator = new DefaultPDNDTokenGenerator with PrivateKeysHolder {
    val RSAPrivateKeyset = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toJSONString)
    val ECPrivateKeyset  = Map.empty
  }

  "a PDNDTokenGenerator" should {

    "generate a valid PDND token" in {

      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      val pdndToken = generator
        .generate(
          clientAssertion = assertion,
          audience = List("test"),
          customClaims = Map("testClaim" -> "hello world"),
          issuerUUID,
          4000L
        )

      pdndToken shouldBe a[Success[_]]

      val signed = SignedJWT.parse(pdndToken.get)

      signed.getJWTClaimsSet.getStringClaim("testClaim") shouldBe "hello world"

      val verifier = new RSASSAVerifier(rsaKey.toRSAKey)
      signed.verify(verifier) shouldBe true
    }

  }

}
