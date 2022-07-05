package it.pagopa.interop.commons.signer.service.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

sealed trait VaultPayload
final case class Response(data: Data)    extends VaultPayload
final case class Data(signature: String) extends VaultPayload

object VaultTransitSerializer extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val dataFormat: RootJsonFormat[Data]         = jsonFormat1(Data)
  implicit val responseFormat: RootJsonFormat[Response] = jsonFormat1(Response)
}
