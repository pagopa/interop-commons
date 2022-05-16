package it.pagopa.interop.commons.vault.service.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ThirdPartyCallError
import it.pagopa.interop.commons.vault.VaultConfig
import it.pagopa.interop.commons.vault.service.VaultTransitService
import it.pagopa.interop.commons.vault.service.impl.VaultTransitSerializer._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

class VaultTransitServiceImpl(val vaultConfig: VaultConfig)(implicit as: ActorSystem) extends VaultTransitService {

  private[this] def vaultHeaders: Seq[HttpHeader] =
    Seq(headers.RawHeader("X-Vault-Token", vaultConfig.token))

  override def encryptData(keyId: String, signatureAlgorithm: Option[String] = None)(data: String): Future[String] = {
    implicit val executionContext: ExecutionContextExecutor = as.getDispatcher

    val signature = signatureAlgorithm.map(s => s"""signature_algorithm: "$s",""").getOrElse("")

    val payload = s"""{
                     |  "input": "$data",
                     |  $signature
                     |  "marshaling_algorithm": "jws"
                     |}""".stripMargin

    val httpEntity = HttpEntity(ContentTypes.`application/json`, payload)

    val responseF = Http().singleRequest(
      HttpRequest(
        uri = vaultConfig.encryptionEndpoint(keyId),
        method = HttpMethods.POST,
        entity = httpEntity,
        headers = vaultHeaders
      )
    )

    responseF
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

}
