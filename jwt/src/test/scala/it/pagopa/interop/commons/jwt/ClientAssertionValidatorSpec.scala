package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import com.nimbusds.jose.jwk.{Curve, ECKey, RSAKey}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.jwt.errors._
import it.pagopa.interop.commons.jwt.model.ValidClientAssertionRequest
import it.pagopa.interop.commons.jwt.service.impl.{DefaultClientAssertionValidator, getClaimsVerifier}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.TryValues._

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.{Date, UUID}
import scala.util.{Failure, Success}

class ClientAssertionValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  object DefaultClientAssertionValidator extends DefaultClientAssertionValidator {
    override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] = getClaimsVerifier()
  }

  val VALID_ASSERTION_TYPE: String = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
  val CLIENT_CREDENTIALS: String   = "client_credentials"

  val rsaKey: RSAKey = new RSAKeyGenerator(2048).generate
  val ecKey: ECKey   = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  "a Client Assertion Validator" should {

    "validate a proper client assertion" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, List("test"), "RSA")

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Success[_]]
    }

    "fail validation when wrong key is retrieved for the client" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, List("test"), "RSA")

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(new RSAKeyGenerator(2048).generate.toPublicJWK.toJSONString)
      } yield ()

      validation.failure.exception should be theSameInstanceAs InvalidJWTSignature
    }

    "fail validation when kid is missing" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion =
        makeJWT(issuerUUID, clientUUID.toString, List("test"), expirationTime, "RSA", kid = null, rsaKey.toJSONString)

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(new RSAKeyGenerator(2048).generate.toPublicJWK.toJSONString)
      } yield ()

      validation.failure.exception should be theSameInstanceAs KidNotFound
    }

    "fail validation when subject is missing" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion =
        makeJWT(
          issuerUUID,
          clientId = null,
          List("test"),
          expirationTime,
          "RSA",
          rsaKey.computeThumbprint().toJSONString,
          rsaKey.toJSONString
        )

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(new RSAKeyGenerator(2048).generate.toPublicJWK.toJSONString)
      } yield ()

      validation.failure.exception should be theSameInstanceAs SubjectNotFound
    }

    "fail validation when no valid assertion type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, List("test"), "RSA")

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = "invalid",
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation.failure.exception shouldBe a[InvalidAccessTokenRequest]
    }

    "fail validation when no valid grant type is provided" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuerUUID, clientUUID.toString, List("test"), "RSA")

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = "auth_code",
        clientId = Some(clientUUID)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation.failure.exception shouldBe a[InvalidAccessTokenRequest]
    }

    "fail validation when assertion is expired" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()
      val audience = List(UUID.randomUUID().toString)

      val rsaKid         = rsaKey.computeThumbprint().toJSONString
      val privateRsaKey  = rsaKey.toJSONString
      val expirationTime = Date.from(OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
      val assertion      = makeJWT(issuer, clientId.toString, audience, expirationTime, "RSA", rsaKid, privateRsaKey)

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- DefaultClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Failure[_]]
    }

    "validate a client assertion for a known audience" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()
      val audience = List("aud1")

      val rsaKid         = rsaKey.computeThumbprint().toJSONString
      val privateRsaKey  = rsaKey.toJSONString
      val expirationTime = Date.from(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
      val assertion      = makeJWT(issuer, clientId.toString, audience, expirationTime, "RSA", rsaKid, privateRsaKey)

      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = Set("aud1"))
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Success[_]]
    }

    "fail validation when audience is unknown" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = Set("aud1"))
      }
      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Failure[_]]
    }

    "validate a client assertion using exact match claims" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(exactMatchClaims =
            Map(JWTClaimNames.ISSUER -> issuer, JWTClaimNames.SUBJECT -> clientId.toString)
          )
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Success[_]]
    }

    "fail validation when exact match claims mismatch" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(exactMatchClaims =
            Map(JWTClaimNames.ISSUER -> UUID.randomUUID().toString, JWTClaimNames.SUBJECT -> clientId.toString)
          )
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Failure[_]]
    }

    "validate a client assertion using required claims" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(requiredClaims = Set(JWTClaimNames.ISSUER))
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Success[_]]
    }

    "fail validation when a required claim is not passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(requiredClaims = Set("nonce"))
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Failure[_]]
    }

    "validate a client assertion when prohibited claims are not passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(prohibitedClaims = Set("nonce"))
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Success[_]]
    }

    "fail validation when prohibited claims are passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()

      val assertion = createMockJWT(rsaKey, issuer, clientId.toString, List("test"), "RSA")
      object CustomClientAssertionValidator extends DefaultClientAssertionValidator {
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(prohibitedClaims = Set(JWTClaimNames.ISSUER))
      }

      val request = ValidClientAssertionRequest.from(
        clientAssertion = assertion,
        clientAssertionType = VALID_ASSERTION_TYPE,
        grantType = CLIENT_CREDENTIALS,
        clientId = Some(clientId)
      )

      val validation = for {
        validRequest <- request
        checker      <- CustomClientAssertionValidator.extractJwtInfo(validRequest)
        _            <- checker.verify(rsaKey.toPublicJWK.toJSONString)
      } yield ()

      validation shouldBe a[Failure[_]]
    }

  }

}
