package it.pagopa.interop.commons.logging

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, provide}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, ACCEPT_LANGUAGE}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class WithLoggingAttributesSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  "Logging attributes directive" should {
    val wrappingDirective: Directive1[Seq[(String, String)]] = provide(Seq.empty[(String, String)])

    "get a Accept-Language if nothing is passed" in {
      Get() ~> withLoggingAttributesF(true)(wrappingDirective) { implicit context =>
        complete {
          val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
          acceptLanguage.length shouldBe 1
          acceptLanguage.head._2 shouldEqual "it-IT"
          "ok"
        }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if it-IT is passed" in {
      val acceptLanguage: String = "it-IT"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "it-IT"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if * is passed" in {
      val acceptLanguage: String = "*"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "it-IT"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if en-EN is passed" in {
      val acceptLanguage: String = "en-EN"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "en-EN"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if it is passed" in {
      val acceptLanguage: String = "it"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "it-IT"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if a not supported language is passed" in {
      val acceptLanguage: String = "de-DE"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "it-IT"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "get a Accept-Language if q value is passed1" in {
      val acceptLanguage: String = "en-EN;q=0.5"
      Get() ~> addHeader(ACCEPT_LANGUAGE, acceptLanguage) ~> withLoggingAttributesF(true)(wrappingDirective) {
        implicit context =>
          complete {
            val acceptLanguage = context.filter(_._1 == ACCEPT_LANGUAGE)
            acceptLanguage.length shouldBe 1
            acceptLanguage.head._2 shouldEqual "en-EN"
            "ok"
          }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "generate a Correlation Id if the service is internet facing and Correlation Id is not given" in {
      Get() ~> withLoggingAttributesF(true)(wrappingDirective) { implicit context =>
        complete {
          val correlationId = context.filter(_._1 == CORRELATION_ID_HEADER)
          correlationId.length shouldBe 1
          correlationId.head._2 should not be empty

          "ok"
        }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "generate a new Correlation Id if the service is internet facing and Correlation Id is given" in {
      val externalCorrelationId: String = UUID.randomUUID().toString
      Get() ~> addHeader(CORRELATION_ID_HEADER, externalCorrelationId) ~> withLoggingAttributesF(true)(
        wrappingDirective
      ) { implicit context =>
        complete {
          val correlationId = context.filter(_._1 == CORRELATION_ID_HEADER)
          correlationId.length shouldBe 1
          correlationId.head._2 should not be externalCorrelationId

          "ok"
        }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "generate a Correlation Id if the service is not internet facing and Correlation Id is not given" in {
      Get() ~> withLoggingAttributesF(false)(wrappingDirective) { implicit context =>
        complete {
          val correlationId = context.filter(_._1 == CORRELATION_ID_HEADER)
          correlationId.length shouldBe 1
          correlationId.head._2 should not be empty

          "ok"
        }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "use the given Correlation Id if the service is not internet facing" in {
      val externalCorrelationId: String = UUID.randomUUID().toString

      Get() ~> addHeader(CORRELATION_ID_HEADER, externalCorrelationId) ~> withLoggingAttributesF(false)(
        wrappingDirective
      ) { implicit context =>
        complete {
          val correlationId = context.filter(_._1 == CORRELATION_ID_HEADER)
          correlationId.length shouldBe 1
          correlationId.head._2 shouldBe externalCorrelationId

          "ok"
        }
      } ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }
  }
}
