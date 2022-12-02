package it.pagopa.interop.commons.utils.errors

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

final case class ProblemError(code: String, detail: String)

object ProblemError extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError.apply)
}
