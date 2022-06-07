package it.pagopa.interop.commons.signer.service.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.commons.signer.VaultConfig
import it.pagopa.interop.commons.signer.model.SignatureAlgorithm
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.VaultTransitSerializer._
import it.pagopa.interop.commons.utils.TypeConversions.{OptionOps, StringOps, TryOps}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ThirdPartyCallError

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

final case class VaultTransitSignerServiceImpl(val vaultConfig: VaultConfig)(implicit as: ActorSystem) extends SignerService {

  private[this] def vaultHeaders: Seq[HttpHeader] =
    Seq(headers.RawHeader("X-Vault-Token", vaultConfig.token))

  override def signData(keyId: String, signatureAlgorithm: SignatureAlgorithm)(data: String): Future[String] = {
    implicit val executionContext: ExecutionContextExecutor = as.getDispatcher

    val response: Future[HttpResponse] = for {
      payload <- createPayload(signatureAlgorithm, data)
      httpEntity = HttpEntity(ContentTypes.`application/json`, payload)
      response <- Http().singleRequest(
        HttpRequest(
          uri = vaultConfig.encryptionEndpoint(keyId),
          method = HttpMethods.POST,
          entity = httpEntity,
          headers = vaultHeaders
        )
      )
    } yield response

    response
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) => Unmarshal(entity).to[Response]
        case HttpResponse(statusCode, _, entity, _)     =>
          entity.discardBytes()
          Future.failed(ThirdPartyCallError("Vault", s"service returned ${statusCode.intValue()}"))
      }
      .flatMap(
        _.data.signature
          .split(":")
          .lastOption
          .toFuture(ThirdPartyCallError("Vault", "service returned not valid signature"))
      )
  }

  private def createPayload(signatureAlgorithm: SignatureAlgorithm, data: String)(implicit
    ec: ExecutionContext
  ): Future[String] = data.encodeBase64.toFuture.map { encodedData =>
    signatureAlgorithm match {
      case SignatureAlgorithm.RSAPkcs1Sha256 | SignatureAlgorithm.RSAPkcs1Sha384 | SignatureAlgorithm.RSAPkcs1Sha512 =>
        s"""{"input": "$encodedData", "signature_algorithm": "pkcs1v15", "marshaling_algorithm": "jws"}"""
      case SignatureAlgorithm.RSAPssSha256 | SignatureAlgorithm.RSAPssSha384 | SignatureAlgorithm.RSAPssSha512       =>
        s"""{"input": "$encodedData", "signature_algorithm": "pss", "marshaling_algorithm": "jws"}"""
      case _ => s"""{"input": "$encodedData", "marshaling_algorithm": "jws"}"""

    }
  }

}
