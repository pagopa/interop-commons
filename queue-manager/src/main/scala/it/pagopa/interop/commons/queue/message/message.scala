package it.pagopa.interop.commons.queue.message

import java.util.UUID
import spray.json._
import spray.json.DefaultJsonProtocol._

trait ProjectableEvent

final case class Message(
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  kind: String,
  payload: ProjectableEvent
)

object Message {
  implicit val uuidFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID  = json match {
      case JsString(x) => UUID.fromString(x)
      case _           => throw new DeserializationException("UUID expected")
    }
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  private def eventJsonReader(f: JsValue => ProjectableEvent): JsonReader[ProjectableEvent] = f

  def messageReader(f: PartialFunction[String, JsValue => ProjectableEvent]): JsonReader[Message] =
    new JsonReader[Message] {
      override def read(json: JsValue): Message = {
        json.asJsObject.getFields(
          "messageUUID",
          "eventJournalPersistenceId",
          "eventJournalSequenceNumber",
          "eventTimestamp",
          "kind",
          "payload"
        ) match {
          case Seq(uuid, ejpi, ejsn, time, kind, payload) =>
            val kindString: String                                 = kind.convertTo[String]
            val deserializer: JsValue => ProjectableEvent          =
              f.applyOrElse(
                kindString,
                (_: String) => throw new DeserializationException(s"Missing mapping for kind $kindString")
              )
            implicit val eventReader: JsonReader[ProjectableEvent] = eventJsonReader(deserializer)

            Message(
              uuid.convertTo[UUID],
              ejpi.convertTo[String],
              ejsn.convertTo[Long],
              time.convertTo[Long],
              kindString,
              payload.convertTo[ProjectableEvent]
            )
          case _ => throw new DeserializationException("Unable to deserialize message structure")
        }
      }
    }

  private def eventJsonWriter(f: PartialFunction[ProjectableEvent, JsValue]): JsonWriter[ProjectableEvent] =
    new JsonWriter[ProjectableEvent] {
      override def write(obj: ProjectableEvent): JsValue =
        f.applyOrElse(
          obj,
          (_: ProjectableEvent) =>
            throw new SerializationException(s"Unmapped kind of event ${obj.getClass().getSimpleName()}")
        )
    }

  def messageWriter(f: PartialFunction[ProjectableEvent, JsValue]): JsonWriter[Message] = new JsonWriter[Message] {
    implicit val eventSerializer: JsonWriter[ProjectableEvent] = eventJsonWriter(f)
    override def write(obj: Message): JsValue                  = JsObject(
      "messageUUID"                -> obj.messageUUID.toJson,
      "eventJournalPersistenceId"  -> obj.eventJournalPersistenceId.toJson,
      "eventJournalSequenceNumber" -> obj.eventJournalSequenceNumber.toJson,
      "eventTimestamp"             -> obj.eventTimestamp.toJson,
      "kind"                       -> obj.kind.toJson,
      "payload"                    -> obj.payload.toJson
    )
  }

  def messageSerde(
    f: PartialFunction[JsValue, ProjectableEvent]
  )(g: PartialFunction[ProjectableEvent, JsValue]): RootJsonFormat[Message] = {
    implicit val eventSerializer: JsonWriter[ProjectableEvent]   = eventJsonWriter(g)
    implicit val eventDeserializer: JsonReader[ProjectableEvent] = eventJsonReader(f)
    implicit val eventSerde: RootJsonFormat[ProjectableEvent]    = new RootJsonFormat[ProjectableEvent] {
      override def read(json: JsValue): ProjectableEvent = eventDeserializer.read(json)
      override def write(obj: ProjectableEvent): JsValue = eventSerializer.write(obj)
    }
    jsonFormat6(Message.apply)
  }

}
