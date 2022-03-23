package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import com.nimbusds.jose.jwk.{Curve, JWK}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.{Date, UUID}
import scala.util.{Failure, Success, Try}

class JWTValidatorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  val rsaKey = new RSAKeyGenerator(2048).generate
  val ecKey  = new ECKeyGenerator(Curve.P_256).generate

  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString
  val publicEcKey: String  = ecKey.toPublicJWK.toJSONString

  val validator = new DefaultJWTReader with PublicKeysHolder {
    var publicKeyset                                                                 = Map(
      rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
      ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
    )
    override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] = getClaimsVerifier()
  }

  "a JWT token validation" should {

    "an EC key should not be parsed as RSA" in {
      val key      = new ECKeyGenerator(Curve.P_256).generate
      val asJSON   = key.toPublicJWK.toJSONString
      val jwk: JWK = JWK.parse(asJSON)
      Try { jwk.toRSAKey } shouldBe a[Failure[_]]
      Try { jwk.toECKey } shouldBe a[Success[_]]
    }

    "a RSA key should not be parsed as EC" in {
      val key      = new RSAKeyGenerator(2048).generate
      val asJSON   = key.toPublicJWK.toJSONString
      val jwk: JWK = JWK.parse(asJSON)
      Try { jwk.toECKey } shouldBe a[Failure[_]]
      Try { jwk.toRSAKey } shouldBe a[Success[_]]
    }

    "properly validate a jwt RSA signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString
      val jwt        = createMockJWT(rsaKey, issuerUUID, clientUUID, List("test"), "RSA")

      val validatedJWT = validator.getClaims(jwt)

      validatedJWT.get.getSubject shouldBe clientUUID
      validatedJWT.get.getIssuer shouldBe issuerUUID
      validatedJWT.get.getAudience should contain only "test"
    }

    "properly validate a jwt EC signed with the corresponding private key" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt = createMockJWT(ecKey, issuerUUID, clientUUID, List("test"), "EC")

      val validatedJWT = validator.getClaims(jwt)

      validatedJWT.get.getSubject shouldBe clientUUID
      validatedJWT.get.getIssuer shouldBe issuerUUID
      validatedJWT.get.getAudience should contain only "test"
    }

    "fail validation if no corresponding key exist in the keys holder" in {
      val issuerUUID = UUID.randomUUID().toString
      val clientUUID = UUID.randomUUID().toString

      val jwt =
        createMockJWT(new ECKeyGenerator(Curve.P_256).generate, issuerUUID, clientUUID, List("test"), "EC")

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

    "fail validation when jwt is expired" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID()
      val audience = List(UUID.randomUUID().toString)

      val rsaKid         = rsaKey.computeThumbprint().toJSONString
      val privateRsaKey  = rsaKey.toJSONString
      val expirationTime = Date.from(OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
      val jwt            = makeJWT(issuer, clientId.toString, audience, expirationTime, "RSA", rsaKid, privateRsaKey)

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

    "properly validate a jwt for a known audience" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString
      val audience = List("aud1")

      val rsaKid         = rsaKey.computeThumbprint().toJSONString
      val privateRsaKey  = rsaKey.toJSONString
      val expirationTime = Date.from(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
      val jwt            = makeJWT(issuer, clientId, audience, expirationTime, "RSA", rsaKid, privateRsaKey)

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = Set("aud1"))
      }

      validator.getClaims(jwt) shouldBe a[Success[_]]
    }

    "fail validation for an unknown audience" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString
      val audience = List("aud2")

      val rsaKid         = rsaKey.computeThumbprint().toJSONString
      val privateRsaKey  = rsaKey.toJSONString
      val expirationTime = Date.from(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 59, ZoneOffset.UTC).toInstant)
      val jwt            = makeJWT(issuer, clientId, audience, expirationTime, "RSA", rsaKid, privateRsaKey)

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = Set("aud1"))
      }

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

    "properly validate a jwt with exact match claims" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(exactMatchClaims = Map(JWTClaimNames.ISSUER -> issuer, JWTClaimNames.SUBJECT -> clientId))
      }

      validator.getClaims(jwt) shouldBe a[Success[_]]
    }

    "fail validation when exact match claims mismatch" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(exactMatchClaims =
            Map(JWTClaimNames.ISSUER -> UUID.randomUUID().toString, JWTClaimNames.SUBJECT -> clientId)
          )
      }

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

    "properly validate a jwt with required claims" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(requiredClaims = Set(JWTClaimNames.ISSUER))
      }

      validator.getClaims(jwt) shouldBe a[Success[_]]
    }

    "fail validation when a required claim is not passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(requiredClaims = Set("nonce"))

      }

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

    "properly validate a jwt when prohibited claims are not passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(prohibitedClaims = Set("nonce"))
      }

      validator.getClaims(jwt) shouldBe a[Success[_]]
    }

    "fail validation when prohibited claims are passed" in {
      val issuer   = UUID.randomUUID().toString
      val clientId = UUID.randomUUID().toString

      val jwt = createMockJWT(rsaKey, issuer, clientId, List("test"), "RSA")

      val validator: DefaultJWTReader = new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset                                                                 = Map(
          rsaKey.computeThumbprint().toJSONString -> rsaKey.toPublicJWK.toJSONString,
          ecKey.computeThumbprint().toJSONString  -> ecKey.toPublicJWK.toJSONString
        )
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(prohibitedClaims = Set(JWTClaimNames.ISSUER))

      }

      validator.getClaims(jwt) shouldBe a[Failure[_]]
    }

  }

}
