package it.pagopa.pdnd.interop.commons.jwt.model

import java.util.UUID
import scala.util.{Try, Success, Failure}
import cats.implicits._
import org.apache.commons.lang3.Validate
import cats.data.Validated
import it.pagopa.pdnd.interop.commons.jwt.errors.InvalidClientAssertionType
import it.pagopa.pdnd.interop.commons.jwt.errors.InvalidGrantType
import cats.data.NonEmptyList
import it.pagopa.pdnd.interop.commons.jwt.errors.InvalidAccessTokenRequest
import cats.data.Validated.{Invalid, Valid}

final case class ClientAssertionRequest private (
  clientAssertion: String,
  clientAssertionType: String,
  grantType: String,
  clientId: Option[UUID]
)

object ClientAssertionRequest {

  private final val jwtBearerClientAssertionType: String =
    "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"
  private final val clientCredentialsGrantType: String = "client_credentials"

  def apply(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Try[ClientAssertionRequest] = validateGrantAndAssertionType(clientAssertionType, grantType)
    .as(new ClientAssertionRequest(clientAssertion, clientAssertionType, grantType, clientId))

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
