package it.pagopa.pdnd.interop.commons.utils

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.commons.jwt.PrivateKeysHolder
import it.pagopa.pdnd.interop.commons.utils.service.impl.DefaultPDNDTokenGenerator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class PDNDTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper with ScalaFutures {

  val rsaKey               = new RSAKeyGenerator(2048).generate
  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString

  val generator = new DefaultPDNDTokenGenerator with PrivateKeysHolder {
    val RSAPrivateKeys = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toJSONString)
    val ECPrivateKeys  = Map.empty
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
        .futureValue

      pdndToken shouldBe a[String]

      val signed = SignedJWT.parse(pdndToken)

      signed.getJWTClaimsSet.getStringClaim("testClaim") shouldBe "hello world"

      val verifier = new RSASSAVerifier(rsaKey.toRSAKey)
      signed.verify(verifier) shouldBe true
    }

  }

}
