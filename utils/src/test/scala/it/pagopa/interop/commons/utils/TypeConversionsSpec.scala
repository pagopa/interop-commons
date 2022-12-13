package it.pagopa.interop.commons.utils

import it.pagopa.interop.commons.utils.TypeConversions._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import org.scalatest.concurrent.PatienceConfiguration

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

  "A future" should {
    "have limited parallelism if using the correct api" in {
      val tp: ExecutorService           = Executors.newFixedThreadPool(21)
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(tp)
      val oneToTwenty: List[Int]        = (1 to 20).toList
      def blockingFuture: Future[Unit]  = Future(Thread.sleep(1000))

      def allTheLimitedFutures(): Future[List[Unit]] =
        Future.traverseWithLatch[Int, Unit](10)(oneToTwenty)(_ => blockingFuture)

      def allTheFutures(): Future[List[Unit]] =
        Future.traverse(oneToTwenty)(_ => blockingFuture)

      assert(!allTheLimitedFutures().isReadyWithin(1000.millis), "The limited future completed too early")
      assert(allTheLimitedFutures().isReadyWithin(2100.millis), "The limited future completed too slowly")
      assert(allTheFutures().isReadyWithin(1100.millis))
      tp.shutdown()
    }

    "failfast using the new traverse method" in {
      val tp: ExecutorService                  = Executors.newFixedThreadPool(20)
      implicit val ec: ExecutionContext        = ExecutionContext.fromExecutor(tp, _ => ())
      val oneToTwenty: List[Int]               = (1 to 20).toList
      def blockingFuture(i: Int): Future[Unit] =
        if (i == 17) Future.failed(new Exception("I'm broken - Pantera")) else Future(Thread.sleep(1000))

      def allTheLimitedFutures(): Future[List[Unit]] =
        Future.traverseWithLatch[Int, Unit](10)(oneToTwenty)(blockingFuture)

      assert(
        allTheLimitedFutures().failed
          .futureValue(PatienceConfiguration.Timeout(2000.millis))
          .getMessage === "I'm broken - Pantera"
      )
      tp.shutdown()
    }
  }

}
