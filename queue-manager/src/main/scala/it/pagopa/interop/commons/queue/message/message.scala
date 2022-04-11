package it.pagopa.interop.commons.queue.message

import java.util.UUID

import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.util.Try
import Message._

final case class Message[T](
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  payload: T
) {
  def asJson(implicit s: JsonSerde[T]): String = {
    implicit val x: RootJsonFormat[T]           = JsonSerde[T].rootJsonFormat
    implicit val mf: RootJsonFormat[Message[T]] = jsonFormat5(Message.apply)
    this.toJson.compactPrint
  }
}

object Message {
  implicit val uuidFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID  = json match {
      case JsString(x) => UUID.fromString(x)
      case _           => throw new DeserializationException("UUID expected")
    }
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  def from[T: JsonSerde](s: String): Either[Throwable, Message[T]] = {
    implicit val x: RootJsonFormat[T]           = JsonSerde[T].rootJsonFormat
    implicit val mf: RootJsonFormat[Message[T]] = jsonFormat5(Message.apply)
    Try(s.parseJson.convertTo[Message[T]]).toEither
  }
}
