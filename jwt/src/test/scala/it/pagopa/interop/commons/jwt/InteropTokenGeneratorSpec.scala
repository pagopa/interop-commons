package it.pagopa.interop.commons.jwt

import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.{Curve, ECKey}
import it.pagopa.interop.commons.jwt.model.Token
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.vault.service.VaultTransitService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MockVaultTransitService extends VaultTransitService {

  override def encryptData(keyId: String)(data: String): Future[String] = {
    // mock signature
    Future.successful("QEp_8a9ePDhqD-4mp-GT0BvzQKOrC8i_SBJhlAcFiqdpoRdpTBvI8IsjJj2uSLzkqZwyUY2gnSZBPNEwQOIRlg")
  }

}

class InteropTokenGeneratorSpec extends AnyWordSpecLike with Matchers with JWTMockHelper with ScalaFutures {

  val ecKey: ECKey         = new ECKeyGenerator(Curve.P_256).generate
  val publicRsaKey: String = ecKey.toPublicJWK.toJSONString

  val generator = new DefaultInteropTokenGenerator(
    MockVaultTransitService,
    new PrivateKeysKidHolder {
      val RSAPrivateKeyset = Set.empty
      val ECPrivateKeyset  = Set(ecKey.computeThumbprint().toJSONString)
    }
  )

  "a InteropTokenGenerator" should {

    "generate a valid Interop token" in {

      val issuerUUID = "me"
      val clientUUID = UUID.randomUUID()

      val assertion = createMockJWT(ecKey, issuerUUID, clientUUID.toString, List("test"), "EC")

      val interopToken = generator
        .generate(clientAssertion = assertion, audience = List("pippo"), customClaims = Map.empty, issuerUUID, 3600L)

      val signed: Token = interopToken.futureValue
      signed shouldBe a[Token]
    }

  }

}
