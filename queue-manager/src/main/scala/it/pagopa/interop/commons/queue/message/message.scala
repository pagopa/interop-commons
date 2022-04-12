package it.pagopa.interop.commons.queue.message

import java.util.UUID
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.util.Try
import Message._

trait Event { val kind: String }

final case class Message(
  messageUUID: UUID,
  eventJournalPersistenceId: String,
  eventJournalSequenceNumber: Long,
  eventTimestamp: Long,
  kind: String,
  payload: Event
)

object Message {
  implicit val uuidFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    override def read(json: JsValue): UUID  = json match {
      case JsString(x) => UUID.fromString(x)
      case _           => throw new DeserializationException("UUID expected")
    }
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  private def eventJsonReader(f: PartialFunction[String, JsValue => Event]): JsonReader[Event] = new JsonReader[Event] {
    override def read(json: JsValue): Event = {
      val maybeKind: Either[Throwable, String] =
        json.asJsObject.getFields("kind").headOption match {
          case Some(JsString(x)) => Right(x)
          case _                 => Left(new Exception("Field kind is required"))
        }

      val maybeDeserializer: Either[Throwable, JsValue => Event] = maybeKind.flatMap(kind =>
        if (f.isDefinedAt(kind)) Right(f(kind))
        else Left(new Exception(s"Missing mapping for kind $kind"))
      )

      maybeDeserializer match {
        case Left(ex) => throw ex
        case Right(f) => f(json)
      }
    }
  }

  private def eventJsonWriter(f: PartialFunction[Event, JsValue]): JsonWriter[Event] = new JsonWriter[Event] {
    override def write(obj: Event): JsValue =
      f.applyOrElse(
        obj,
        (_: Event) => throw new Exception(s"Unmapped kind of event ${obj.getClass().getCanonicalName()}")
      )
  }

  def messageWriter(f: PartialFunction[Event, JsValue]): JsonWriter[Message] = new JsonWriter[Message] {
    implicit val eventSerializer: JsonWriter[Event] = eventJsonWriter(f)
    override def write(obj: Message): JsValue       = JsObject(
      "messageUUID"                -> obj.messageUUID.toJson,
      "eventJournalPersistenceId"  -> obj.eventJournalPersistenceId.toJson,
      "eventJournalSequenceNumber" -> obj.eventJournalSequenceNumber.toJson,
      "eventTimestamp"             -> obj.eventTimestamp.toJson,
      "kind"                       -> obj.kind.toJson,
      "payload"                    -> obj.payload.toJson
    )
  }

  def messageReader(f: PartialFunction[String, JsValue => Event]): JsonReader[Message] = new JsonReader[Message] {
    implicit val eventDeserializer: JsonReader[Event] = eventJsonReader(f)
    override def read(json: JsValue): Message         = {
      json.asJsObject.getFields(
        "messageUUID",
        "eventJournalPersistenceId",
        "eventJournalSequenceNumber",
        "eventTimestamp",
        "kind",
        "payload"
      ) match {
        case Seq(uuid, ejpi, ejsn, time, kind, payload) =>
          Message(
            uuid.convertTo[UUID],
            ejpi.convertTo[String],
            ejsn.convertTo[Long],
            time.convertTo[Long],
            kind.convertTo[String],
            payload.convertTo[Event]
          )
        case _                                          => throw new Exception("Unable to deserialize message")
      }
    }
  }

  def messageSerde(
    f: PartialFunction[String, JsValue => Event]
  )(g: PartialFunction[Event, JsValue]): RootJsonFormat[Message] = {
    implicit val eventSerializer: JsonWriter[Event]   = eventJsonWriter(g)
    implicit val eventDeserializer: JsonReader[Event] = eventJsonReader(f)
    implicit val eventSerde: RootJsonFormat[Event]    = new RootJsonFormat[Event] {
      override def read(json: JsValue): Event = eventDeserializer.read(json)
      override def write(obj: Event): JsValue = eventSerializer.write(obj)
    }
    jsonFormat6(Message.apply)
  }

}
