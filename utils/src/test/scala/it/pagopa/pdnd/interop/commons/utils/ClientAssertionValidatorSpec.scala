package it.pagopa.pdnd.interop.commons.utils

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import it.pagopa.pdnd.interop.commons.errors.InvalidJWTSignature
import it.pagopa.pdnd.interop.commons.jwt.PublicKeysHolder
import it.pagopa.pdnd.interop.commons.utils.service.impl.DefaultClientAssertionValidator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientAssertionValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper with ScalaFutures {

  val VALID_ASSERTION_TYPE: String = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"
  val CLIENT_CREDENTIALS: String   = "client_credentials"

  val rsaKey = new RSAKeyGenerator(2048).generate
  val ecKey  = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  val validator = new DefaultClientAssertionValidator with PublicKeysHolder {
    val RSAPublicKeys = Map(rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString)
    val ECPublicKeys  = Map(ecKey.computeThumbprint().toJSONString -> ecKey.toPublicJWK.toJSONString)
  }

  "a Client Assertion Validator" should {

    "validate a proper client assertion" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      validator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = CLIENT_CREDENTIALS,
          clientId = Some(clientUUID)
        )((_, _) => Future.successful(rsaKey.toPublicJWK.toJSONString))
        .futureValue shouldBe (clientUUID.toString -> true)
    }

    "fail validation when wrong key is retrieved for the client" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      validator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = CLIENT_CREDENTIALS,
          clientId = Some(clientUUID)
        )((_, _) => Future.successful(new RSAKeyGenerator(2048).generate.toPublicJWK.toJSONString))
        .failed
        .futureValue shouldBe InvalidJWTSignature
    }

    "fail validation when no valid assertion type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      validator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = "invalid",
          grantType = CLIENT_CREDENTIALS,
          clientId = Some(clientUUID)
        )((_, _) => Future.successful(rsaKey.toPublicJWK.toJSONString))
        .failed
        .futureValue shouldBe a[Throwable]
    }

    "fail validation when no valid grant type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, "test", "RSA")

      validator
        .validate(
          clientAssertion = assertion,
          clientAssertionType = VALID_ASSERTION_TYPE,
          grantType = "auth_code",
          clientId = Some(clientUUID)
        )((_, _) => Future.successful(rsaKey.toPublicJWK.toJSONString))
        .failed
        .futureValue shouldBe a[Throwable]
    }

  }

}
