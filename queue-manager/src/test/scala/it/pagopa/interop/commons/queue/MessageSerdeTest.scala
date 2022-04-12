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
import it.pagopa.interop.commons.queue.message.Named

class MessageSerdeTest extends AnyWordSpecLike with Matchers {

  "Message" should {
    "be converted" in {
      // val string = Message.jsonWriter
      //   .write(Message(UUID.randomUUID(), "persId", 1L, 100L, ThingCreated(UUID.randomUUID(), "thingName")))
      // string shouldBe "ciao"
    }

    "be deconverted" in {
      // Message.jsonReader { case "things_created" => serde }
    }
  }

}

object MessageSerdeTest {

  final case class ThingCreated(thingUUID: UUID, thingName: String)

  implicit val named: Named[ThingCreated] = new Named[ThingCreated] {
    val kind = "things_created"
  }

  val json: String =
    """{"eventJournalPersistenceId":"persId","eventJournalSequenceNumber":1,"eventTimestamp":100,"kind":"things_created","messageUUID":"a2781d00-2a4a-4b16-9281-63ad2568ec5b","payload":{"thingName":"thingName","thingUUID":"db3fba92-cc07-4725-a1c9-c5f1ad415955"}}"""

}
