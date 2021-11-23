package it.pagopa.pdnd.interop.commons.utils

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.utils.service.impl.DefaultJWTValidator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class JWTValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper with ScalaFutures {

  val rsaKey = new RSAKeyGenerator(2048).generate
  val ecKey  = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  val validator = new DefaultJWTValidator with PublicKeysHolder {
    val RSAPublicKeyset = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
    val ECPublicKeyset  = Map(ecKey.computeThumbprint().toJSONString -> ecKey.toPublicJWK.toJSONString)
  }

  "a JWT token validation" should {

    "properly validate a jwt RSA signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString
      val jwt        = createMockJWT(rsaKey, issuerUUID, clientUUID, "test", "RSA")

      val validatedJWT = validator.validate(jwt).futureValue

      validatedJWT.getSubject shouldBe clientUUID
      validatedJWT.getIssuer shouldBe issuerUUID
      validatedJWT.getAudience should contain only "test"
    }

    "properly validate a jwt EC signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt = createMockJWT(ecKey, issuerUUID, clientUUID, "test", "EC")

      val validatedJWT = validator.validate(jwt).futureValue

      validatedJWT.getSubject shouldBe clientUUID
      validatedJWT.getIssuer shouldBe issuerUUID
      validatedJWT.getAudience should contain only "test"
    }

    "fail validation if no corresponding key exist in the keys holder" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt =
        createMockJWT(new ECKeyGenerator(Curve.P_256).generate, issuerUUID, clientUUID, "test", "EC")

      validator.validate(jwt).failed.futureValue shouldBe a[Throwable]
    }

  }

}
