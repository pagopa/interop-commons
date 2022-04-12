package it.pagopa.interop.commons.queue

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import java.util.UUID
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import it.pagopa.interop.commons.queue.message.Message.uuidFormat
import it.pagopa.interop.commons.queue.message.Message
import MessageSerdeTest._
import it.pagopa.interop.commons.queue.message.Event

class MessageSerdeTest extends AnyWordSpecLike with Matchers {

  "Message" should {
    "be converted" in {
      val string = Message(
        UUID.randomUUID(),
        "persId",
        1L,
        100L,
        "thing_created",
        ThingCreated("thing_created", UUID.randomUUID(), "thingName")
      ).toJson.compactPrint
      string shouldBe "ciao"
    }

    "be deconverted" in {
      println(json.parseJson.convertTo[Message])
    }
  }

}

object MessageSerdeTest {

  final case class ThingCreated(kind: String, thingUUID: UUID, thingName: String) extends Event
  final case class AnotherThing(kind: String, thingUUID: UUID, thingName: String) extends Event

  val thingCreatedFormat: RootJsonFormat[ThingCreated] = jsonFormat3(ThingCreated.apply)
  val anotherThingFormat: RootJsonFormat[AnotherThing] = jsonFormat3(AnotherThing.apply)

  val f: PartialFunction[String, JsValue => Event] = {
    case "thing_created" => _.convertTo[ThingCreated](thingCreatedFormat)
    case "another_thing" => _.convertTo[AnotherThing](anotherThingFormat)
  }

  val g: PartialFunction[Event, JsValue] = {
    case x: ThingCreated => x.toJson(thingCreatedFormat)
    case x: AnotherThing => x.toJson(anotherThingFormat)
  }

  implicit val messageReader: JsonReader[Message] = Message.messageReader(f)
  implicit val messageWriter: JsonWriter[Message] = Message.messageWriter(g)

  val json: String =
    """{"eventJournalPersistenceId":"persId","eventJournalSequenceNumber":1,"eventTimestamp":100,"kind":"thing_created","messageUUID":"4129a4dd-0596-48dd-a975-dc5bd0022329","payload":{"kind":"thing_created","thingName":"thingName","thingUUID":"9cc6e73c-3c50-4da1-926c-f674f50f3677"}}"""

}
