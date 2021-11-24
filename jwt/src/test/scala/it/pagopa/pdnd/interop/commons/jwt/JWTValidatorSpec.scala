package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import it.pagopa.pdnd.interop.commons.jwt.service.impl.DefaultJWTReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.Failure

class JWTValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val rsaKey = new RSAKeyGenerator(2048).generate
  val ecKey  = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  val validator = new DefaultJWTReader with PublicKeysHolder {
    val RSAPublicKeyset = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
    val ECPublicKeyset  = Map(ecKey.computeThumbprint().toJSONString -> ecKey.toPublicJWK.toJSONString)
  }

  "a JWT token validation" should {

    "properly validate a jwt RSA signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString
      val jwt        = createMockJWT(rsaKey, issuerUUID, clientUUID, "test", "RSA")

      val validatedJWT = validator.getClaims(jwt)

      validatedJWT.get.getSubject shouldBe clientUUID
      validatedJWT.get.getIssuer shouldBe issuerUUID
      validatedJWT.get.getAudience should contain only "test"
    }

    "properly validate a jwt EC signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt = createMockJWT(ecKey, issuerUUID, clientUUID, "test", "EC")

      val validatedJWT = validator.getClaims(jwt)

      validatedJWT.get.getSubject shouldBe clientUUID
      validatedJWT.get.getIssuer shouldBe issuerUUID
      validatedJWT.get.getAudience should contain only "test"
    }

    "fail validation if no corresponding key exist in the keys holder" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt =
        createMockJWT(new ECKeyGenerator(Curve.P_256).generate, issuerUUID, clientUUID, "test", "EC")

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

  }

}
