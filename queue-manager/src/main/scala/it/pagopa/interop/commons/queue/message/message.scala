package it.pagopa.interop.commons.queue.message

import java.util.UUID
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.util.Try
import Message._

trait Named[T] { val kind: String }
object Named   {
  def apply[T](implicit x: Named[T]): Named[T] = x
}

final case class Message[T: Named](
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  payload: T
)

object Message {
  implicit val uuidFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID  = json match {
      case JsString(x) => UUID.fromString(x)
      case _           => throw new DeserializationException("UUID expected")
    }
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  def jsonWriter[T: Named](implicit x: RootJsonFormat[T]): JsonWriter[Message[T]] = new JsonWriter[Message[T]] {
    override def write(m: Message[T]): JsValue = JsObject(
      "messageUUID"                -> JsString(m.messageUUID.toString),
      "eventJournalPersistenceId"  -> JsString(m.eventJournalPersistenceId),
      "eventJournalSequenceNumber" -> JsNumber(m.eventJournalSequenceNumber),
      "eventTimestamp"             -> JsNumber(m.eventTimestamp),
      "kind"                       -> JsString(Named[T].kind),
      "payload"                    -> m.payload.toJson
    )
  }

  def jsonReader[T: Named](deser: PartialFunction[String, RootJsonFormat[T]]): JsonReader[Message[T]] =
    new JsonReader[Message[T]] {
      override def read(json: JsValue): Message[T] = {
        val jsObject: JsObject = json.asJsObject

        implicit val eventDeserializer: RootJsonFormat[T] = jsObject.getFields("kind") match {
          case Seq(JsString(x)) =>
            deser.applyOrElse(x, throw new DeserializationException(s"Unable to deserialize Event $x"))
          case _                => throw new DeserializationException("kind expected")
        }

        jsObject.getFields(
          "messageUUID",
          "eventJournalPersistenceId",
          "eventJournalSequenceNumber",
          "eventTimestamp",
          "payload"
        ) match {
          case Seq(uuidS, JsString(ejpId), ejsn, time, payload: JsObject) =>
            Message(uuidS.convertTo[UUID], ejpId, ejsn.convertTo[Long], time.convertTo[Long], payload.convertTo[T])
          case _ => throw new DeserializationException("Unable to deserialize Message")
        }
      }
    }

}
