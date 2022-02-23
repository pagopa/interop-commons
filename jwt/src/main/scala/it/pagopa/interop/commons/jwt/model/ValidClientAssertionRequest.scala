package it.pagopa.interop.commons.jwt.model

import cats.data.{NonEmptyList, Validated}
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import it.pagopa.interop.commons.jwt.errors.{InvalidAccessTokenRequest, InvalidClientAssertionType, InvalidGrantType}

import java.util.UUID
import scala.util.{Failure, Success, Try}

final case class ValidClientAssertionRequest private (
  clientAssertion: String,
  clientAssertionType: String,
  grantType: String,
  clientId: Option[UUID]
)

object ValidClientAssertionRequest {

  private final val jwtBearerClientAssertionType: String =
    "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"
  private final val clientCredentialsGrantType: String = "client_credentials"

  def from(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Try[ValidClientAssertionRequest] = validateGrantAndAssertionType(clientAssertionType, grantType)
    .as(new ValidClientAssertionRequest(clientAssertion, clientAssertionType, grantType, clientId))

  private def validateGrantAndAssertionType(clientAssertionType: String, grantType: String): Try[(String, String)] = {
    val validClientAssertion: Validated[NonEmptyList[Throwable], String] = Validated
      .validNel(clientAssertionType)
      .ensureOr(s => NonEmptyList.one(InvalidClientAssertionType(s"Client assertion type '$s' is not valid")))(
        _ == jwtBearerClientAssertionType
      )

    val validGrantType: Validated[NonEmptyList[Throwable], String] = Validated
      .validNel(grantType)
      .ensureOr(s => NonEmptyList.one(InvalidGrantType(s)))(_ == clientCredentialsGrantType)

    (validClientAssertion, validGrantType).tupled match {
      case Invalid(e) => Failure(InvalidAccessTokenRequest(e.map(_.getMessage).toList))
      case Valid(a)   => Success(a)
    }
  }

}
