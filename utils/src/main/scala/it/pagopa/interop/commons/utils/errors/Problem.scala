package it.pagopa.interop.commons.utils.errors

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import spray.json._

final case class Problem(
  `type`: String,
  status: Int,
  title: String,
  detail: Option[String] = None,
  errors: Seq[ProblemError]
)

object Problem extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def problemFormat: RootJsonFormat[Problem] = jsonFormat5(Problem.apply)
  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val defaultProblemType: String  = "about:blank"
  final val defaultErrorMessage: String = "Unknown error"

  def apply(httpError: StatusCode, error: ComponentError, serviceErrorCodePrefix: String): Problem = Problem(
    `type` = defaultProblemType,
    status = httpError.intValue,
    title = httpError.defaultMessage,
    errors = Seq(
      ProblemError(
        code = s"$serviceErrorCodePrefix-${error.code}",
        detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
      )
    )
  )
}
