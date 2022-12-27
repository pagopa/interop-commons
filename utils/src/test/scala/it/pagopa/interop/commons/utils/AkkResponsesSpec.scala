package it.pagopa.interop.commons.utils

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, StatusCode, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.errors.AkkaResponses._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError
import it.pagopa.interop.commons.utils.errors.{ComponentError, Problem, ProblemError, ServiceCode}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

private[this] final case class NewComponentError(message: String) extends ComponentError("xyz", message)

class AkkResponsesSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  val correlationId: String = "some-correlation-id"
  val error: ComponentError = NewComponentError("The ComponentError message")
  val logMessage: String    = "The log message"

  implicit val serviceCode: ServiceCode                         = ServiceCode("999")
  implicit val contexts: Seq[(String, String)]                  = Seq(CORRELATION_ID_HEADER -> correlationId)
  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def problem(statusCode: StatusCode, error: ComponentError): Problem = Problem(
    `type` = Problem.defaultProblemType,
    status = statusCode.intValue(),
    title = statusCode.defaultMessage(),
    correlationId = Some(correlationId),
    detail = None,
    errors = Seq(ProblemError(code = s"${serviceCode.code}-${error.code}", detail = error.msg))
  )

//  def verifyResponse(expectedStatusCode: StatusCode): Assertion = {
//    status shouldEqual expectedStatusCode
//    responseAs[Problem] shouldEqual problem(expectedStatusCode, error)
//  }

  def verifyResponse(expectedStatusCode: StatusCode): RouteTestResult => Assertion =
    check {
      status shouldEqual expectedStatusCode
      responseAs[Problem] shouldEqual problem(expectedStatusCode, error)
    }

  "Expected error response" should {
    "be returned in case of Bad Request (single error)" in {
      Get() ~> badRequest(error, logMessage) ~> verifyResponse(StatusCodes.BadRequest)
    }

    "be returned in case of Bad Request (multiple errors)" in {
      val otherError                   = NewComponentError("Another error message")
      val errors: List[ComponentError] = List(error, otherError)

      Get() ~> badRequest(errors, logMessage) ~> check {
        val expectedStatus  = StatusCodes.BadRequest
        val baseProblem     = problem(expectedStatus, error)
        val expectedProblem = baseProblem.copy(errors =
          baseProblem.errors :+ ProblemError(code = s"${serviceCode.code}-${otherError.code}", detail = otherError.msg)
        )

        status shouldEqual expectedStatus
        responseAs[Problem] shouldEqual expectedProblem
      }
    }

    "be returned in case of Unauthorized" in {
      Get() ~> unauthorized(error, logMessage) ~> verifyResponse(StatusCodes.Unauthorized)
    }

    "be returned in case of Forbidden" in {
      Get() ~> forbidden(error, logMessage) ~> verifyResponse(StatusCodes.Forbidden)
    }

    "be returned in case of Not Found" in {
      Get() ~> notFound(error, logMessage) ~> verifyResponse(StatusCodes.NotFound)
    }

    "be returned in case of Conflict" in {
      Get() ~> conflict(error, logMessage) ~> verifyResponse(StatusCodes.Conflict)
    }

    "be returned in case of Too Many Requests" in {
      val expectedHeaders = List[HttpHeader](RawHeader("header-1", "value-1"), RawHeader("header-2", "value-2"))
      Get() ~> tooManyRequests(error, logMessage, expectedHeaders) ~> check {
        val expectedStatusCode = StatusCodes.TooManyRequests
        status shouldEqual expectedStatusCode
        responseAs[Problem] shouldEqual problem(expectedStatusCode, error)
        headers should contain allElementsOf expectedHeaders
      }
    }

    "be returned in case of Internal Server Error" in {
      val exception              = new Throwable("An unexpected exception")
      val expectedComponentError = GenericError(logMessage)
      val expectedProblemError   =
        ProblemError(code = s"${serviceCode.code}-${expectedComponentError.code}", detail = expectedComponentError.msg)
      Get() ~> internalServerError(exception, logMessage) ~> check {
        val expectedStatusCode = StatusCodes.InternalServerError
        status shouldEqual expectedStatusCode
        responseAs[Problem] shouldEqual problem(expectedStatusCode, error).copy(errors = Seq(expectedProblemError))
      }
    }
  }
}
