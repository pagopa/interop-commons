package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import it.pagopa.interop.commons.jwt.service.impl.DefaultSessionTokenGenerator
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm
import it.pagopa.interop.commons.signer.service.SignerService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MockSessionVaultTransitService extends SignerService {

  override def signData(keyId: String, signatureAlgorithm: SignatureAlgorithm)(data: String): Future[String] = {
    // mock signature
    Future.successful("QEp_8a9ePDhqD-4mp-GT0BvzQKOrC8i_SBJhlAcFiqdpoRdpTBvI8IsjJj2uSLzkqZwyUY2gnSZBPNEwQOIRlg")
  }

}

class SessionTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper with ScalaFutures {

  val rsaKey               = new RSAKeyGenerator(2048).generate
  val publicRsaKey: String = rsaKey.toPublicJWK.toJSONString

  val generator = new DefaultSessionTokenGenerator(
    MockSessionVaultTransitService,
    new PrivateKeysKidHolder {
      val RSAPrivateKeyset = Set(rsaKey.computeThumbprint().toJSONString)
      val ECPrivateKeyset  = Set.empty
    }
  )

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
          signatureAlgorithm = SignatureAlgorithm.RSAPkcs1Sha256,
          claimsSet = claimsSet,
          audience = Set("test"),
          tokenIssuer = issuerUUID,
          validityDurationInSeconds = 4000L
        )

      val result = sessionToken.futureValue

      result shouldBe a[String]
      val signed = SignedJWT.parse(result)

      signed.getJWTClaimsSet.getStringClaim("uid") shouldBe uid
      signed.getJWTClaimsSet
        .getClaim("organization")
        .asInstanceOf[util.Map[String, String]] shouldBe organizationClaims
    }

  }

}
