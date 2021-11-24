package it.pagopa.pdnd.interop.commons.jwt

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import it.pagopa.pdnd.interop.commons.jwt.service.impl.DefaultClientAssertionValidator
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.{Failure, Success}

class ClientAssertionValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val VALID_ASSERTION_TYPE: String = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"
  val CLIENT_CREDENTIALS: String   = "client_credentials"

  val rsaKey = new RSAKeyGenerator(2048).generate
  val ecKey  = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  "a Client Assertion Validator" should {

    "validate a proper client assertion" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      val validation = DefaultClientAssertionValidator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = CLIENT_CREDENTIALS,
          clientUUID = Some(clientUUID),
          clientKeys = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
        )
      validation shouldBe a[Success[_]]
    }

    "fail validation when wrong key is retrieved for the client" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      val validation = DefaultClientAssertionValidator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = CLIENT_CREDENTIALS,
          clientUUID = Some(clientUUID),
          clientKeys =
            Map(rsaKey.computeThumbprint().toJSONString -> new RSAKeyGenerator(2048).generate.toPublicJWK.toJSONString)
        )

      validation shouldBe a[Failure[_]]
    }

    "fail validation when no valid assertion type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      val validation = DefaultClientAssertionValidator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = "invalid",
          grantType = CLIENT_CREDENTIALS,
          clientUUID = Some(clientUUID),
          clientKeys = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
        )

      validation shouldBe a[Failure[_]]
    }

    "fail validation when no valid grant type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      val validation = DefaultClientAssertionValidator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = "auth_code",
          clientUUID = Some(clientUUID),
          clientKeys = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
        )

      validation shouldBe a[Failure[_]]
    }

  }

}
