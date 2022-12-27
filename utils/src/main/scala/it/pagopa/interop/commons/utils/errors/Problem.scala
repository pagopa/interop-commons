package it.pagopa.interop.commons.utils.errors

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import spray.json._

final case class Problem(
  `type`: String,
  status: Int,
  title: String,
  correlationId: Option[String],
  detail: Option[String] = None,
  errors: Seq[ProblemError]
)

object Problem extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def problemFormat: RootJsonFormat[Problem]                 = jsonFormat6(Problem.apply)
  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val defaultProblemType: String  = "about:blank"
  final val defaultErrorMessage: String = "Unknown error"

  def apply(
    httpError: StatusCode,
    error: ComponentError,
    serviceCode: ServiceCode,
    correlationId: Option[String]
  ): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      correlationId = correlationId,
      errors = Seq(
        ProblemError(
          code = s"${serviceCode.code}-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def apply(
    httpError: StatusCode,
    errors: List[ComponentError],
    serviceCode: ServiceCode,
    correlationId: Option[String]
  ): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      correlationId = correlationId,
      errors = errors.map(error =>
        ProblemError(
          code = s"${serviceCode.code}-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

}
