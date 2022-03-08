package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.Success

class InteropTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val rsaKey               = new RSAKeyGenerator(2048).generate
  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString

  val generator = new DefaultInteropTokenGenerator with PrivateKeysHolder {
    val RSAPrivateKeyset = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toJSONString)
    val ECPrivateKeyset  = Map.empty
  }

  "a InteropTokenGenerator" should {

    "generate a valid Interop token" in {

      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()
      val subject    = UUID.randomUUID().toString

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, List("test"), "RSA")

      val interopToken = generator
        .generate(
          clientAssertion = assertion,
          subject = subject,
          audience = List("test"),
          customClaims = Map("testClaim" -> "hello world"),
          issuerUUID,
          4000L
        )

      interopToken shouldBe a[Success[_]]

      val signed = SignedJWT.parse(interopToken.get)

      signed.getJWTClaimsSet.getStringClaim("testClaim") shouldBe "hello world"

      val verifier = new RSASSAVerifier(rsaKey.toRSAKey)
      signed.verify(verifier) shouldBe true
    }

  }

}