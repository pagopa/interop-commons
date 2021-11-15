package it.pagopa.pdnd.interop.commons.utils

import it.pagopa.pdnd.interop.commons.utils.TypeConversions._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class TypeConvertionsSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "an OffsetDateTime" should {
    "be converted to a String" in {
      OffsetDateTime
        .of(2021, 1, 1, 10, 10, 10, 0, ZoneOffset.UTC)
        .asFormattedString shouldBe Success("2021-01-01T10:10:10Z")
    }
  }

  "a String type" should {
    "be converted to UUID" in {
      "bf80fac0-2775-4646-8fcf-28e083751901".toUUID shouldBe a[Success[_]]
    }

    "fail when a UUID is invalid" in {
      "bf80fac0-2775-4646-8fcf-28e0837519XX".toUUID shouldBe a[Failure[_]]
    }

    "be converted to future UUID" in {
      val r: Future[UUID] = "bf80fac0-2775-4646-8fcf-28e083751901".toFutureUUID
      r.futureValue shouldBe a[UUID]
    }

    "fail the future UUID convertion when the string is invalid" in {
      "bf80fac0-2775-4646-8fcf-28e0837519XX".toFutureUUID.failed.futureValue shouldBe a[Throwable]
    }

    "be converted to OffsetDateTime" in {
      "2011-12-03T10:15:30+01:00".toOffsetDateTime shouldBe a[Success[_]]
    }

    "fail OffsetDateTime when no valid input is provided" in {
      "2011-12-03Q10:15:30+01:00".toOffsetDateTime shouldBe a[Failure[_]]
    }

    "parse comma separated string" in {
      "a, b, c, d, e".parseCommaSeparated should contain only ("a", "b", "c", "d", "e")
    }

    "parse comma separated string without commas" in {
      "a".parseCommaSeparated should contain only "a"
    }

    "parse comma separated empty string" in {
      "".parseCommaSeparated shouldBe empty
    }

    "decode string to UTF-8 base64" in {
      "UERORCBJbnTDqHLDsnAgUsOWY2tzIQ==".decodeBase64 shouldBe Success("PDND Intèròp RÖcks!")
    }

    "interpolate a string with variables" in {
      "${friend}, come stai?" interpolate Map("friend" -> "Pippo") shouldBe "Pippo, come stai?"
    }

    "interpolate a string with missing variables" in {
      "${friend}, come stai?" interpolate Map("friendOne" -> "Pippo") shouldBe "${friend}, come stai?"
    }

    "interpolate a string with empty map of variables" in {
      "${friend}, come stai?" interpolate Map.empty shouldBe "${friend}, come stai?"
    }
  }
}
