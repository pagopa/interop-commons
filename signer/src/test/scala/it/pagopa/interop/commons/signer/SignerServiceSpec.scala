package it.pagopa.interop.commons.signer

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.ScalaFutures._
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm.RSAPkcs1Sha256
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ThirdPartyCallError

import scala.concurrent.{Future, ExecutionContext}

class SignerServiceSpec extends AnyWordSpecLike with MockFactory {
  val mockSignerService: SignerService  = mock[SignerService]
  implicit val global: ExecutionContext = ExecutionContext.global

  "Signer service" should {
    "succeed" in {

      val data: String = "yadayada"

      (mockSignerService
        .signData(_: String, _: SignatureAlgorithm)(_: String))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(data))

      mockSignerService
        .signData("keyId", RSAPkcs1Sha256)("data")
        .map(_ shouldEqual data)

    }

    "fail with exception " in {

      (mockSignerService
        .signData(_: String, _: SignatureAlgorithm)(_: String))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(ThirdPartyCallError("KMS", "message")))

      mockSignerService
        .signData("keyId", RSAPkcs1Sha256)("data")
        .failed
        .futureValue shouldBe ThirdPartyCallError("KMS", "message")

    }
  }
}
