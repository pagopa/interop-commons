package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.TypeConversions._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class TypeConversionsSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "an OffsetDateTime" should {
    "be converted to a String" in {
      OffsetDateTime
        .of(2021, 1, 1, 10, 10, 10, 0, ZoneOffset.UTC)
        .asFormattedString shouldBe Success("2021-01-01T10:10:10Z")
    }

    "be converted to a Long" in {
      OffsetDateTime
        .of(2021, 1, 1, 10, 10, 10, 0, ZoneOffset.UTC)
        .toMillis shouldBe 1609495810000L
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
      "SW50w6hyw7JwIFLDlmNrcyE=".decodeBase64 shouldBe Success("Intèròp RÖcks!")
    }

    "interpolate a string with variables" in {
      "${friend}, how are you?" interpolate Map("friend" -> "John Doe") shouldBe "John Doe, how are you?"
    }

    "interpolate a string with missing variables" in {
      "${friend}, how are you?" interpolate Map("friendOne" -> "John Doe") shouldBe "${friend}, how are you?"
    }

    "interpolate a string with empty map of variables" in {
      "${friend}, how are you?" interpolate Map.empty shouldBe "${friend}, how are you?"
    }
  }

  "a Long" should {
    "be converted to an OffsetDateTime" in {
      1609495810000L.toOffsetDateTime shouldBe
        Success(
          OffsetDateTime
            .of(2021, 1, 1, 10, 10, 10, 0, ZoneOffset.UTC)
        )
    }

  }
}
