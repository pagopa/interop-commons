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
import it.pagopa.interop.commons.queue.message.ProjectableEvent

class MessageSerdeTest extends AnyWordSpecLike with Matchers {

  val testMessage1      =
    Message(
      UUID.fromString("4129a4dd-0596-48dd-a975-dc5bd0022329"),
      "persId",
      1L,
      100L,
      "thing_created",
      ThingCreated(UUID.fromString("4129a4dd-0596-48dd-a975-dc5bd0022329"), "thingName")
    )
  val testJson1: String =
    """{"eventJournalPersistenceId":"persId","eventJournalSequenceNumber":1,"eventTimestamp":100,"kind":"thing_created","messageUUID":"4129a4dd-0596-48dd-a975-dc5bd0022329","payload":{"thingName":"thingName","thingUUID":"4129a4dd-0596-48dd-a975-dc5bd0022329"}}"""

  val testMessage2 =
    Message(
      UUID.fromString("4129a4dd-0596-48dd-a975-dc5bd0022329"),
      "persId",
      1L,
      100L,
      "another_thing",
      AnotherThing(UUID.fromString("4129a4dd-0596-48dd-a975-dc5bd0022329"), "thingName")
    )

  val testJson2: String =
    """{"eventJournalPersistenceId":"persId","eventJournalSequenceNumber":1,"eventTimestamp":100,"kind":"another_thing","messageUUID":"4129a4dd-0596-48dd-a975-dc5bd0022329","payload":{"thingName":"thingName","thingUUID":"4129a4dd-0596-48dd-a975-dc5bd0022329"}}"""

  "Message" should {
    "be converted" in {
      testMessage1.toJson.compactPrint shouldBe testJson1
      testMessage2.toJson.compactPrint shouldBe testJson2
    }

    "be deconverted" in {
      val parsedMessage1: Message = testJson1.parseJson.convertTo[Message]
      val parsedMessage2: Message = testJson2.parseJson.convertTo[Message]
      parsedMessage1 shouldBe testMessage1
      parsedMessage2 shouldBe testMessage2

      val payloads: List[ProjectableEvent] = List(parsedMessage1, parsedMessage2).map(_.payload)
      payloads.collect { case x: ThingCreated => x } shouldBe parsedMessage1.payload :: Nil
      payloads.collect { case x: AnotherThing => x } shouldBe parsedMessage2.payload :: Nil
    }
  }

}

object MessageSerdeTest {

  final case class ThingCreated(thingUUID: UUID, thingName: String) extends ProjectableEvent
  final case class AnotherThing(thingUUID: UUID, thingName: String) extends ProjectableEvent

  val thingCreatedFormat: RootJsonFormat[ThingCreated] = jsonFormat2(ThingCreated.apply)
  val anotherThingFormat: RootJsonFormat[AnotherThing] = jsonFormat2(AnotherThing.apply)

  val f: PartialFunction[String, JsValue => ProjectableEvent] = {
    case "thing_created" => _.convertTo[ThingCreated](thingCreatedFormat)
    case "another_thing" => _.convertTo[AnotherThing](anotherThingFormat)
  }

  val g: PartialFunction[ProjectableEvent, JsValue] = {
    case x: ThingCreated => x.toJson(thingCreatedFormat)
    case x: AnotherThing => x.toJson(anotherThingFormat)
  }

  implicit val messageReader: JsonReader[Message] = Message.messageReader(f)
  implicit val messageWriter: JsonWriter[Message] = Message.messageWriter(g)
}
