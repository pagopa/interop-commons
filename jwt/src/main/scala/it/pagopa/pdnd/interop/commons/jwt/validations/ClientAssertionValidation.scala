package it.pagopa.pdnd.interop.commons.jwt.validations

import it.pagopa.pdnd.interop.commons.jwt.errors.{
  InvalidAccessTokenRequest,
  InvalidClientAssertionType,
  InvalidGrantType
}

import scala.util.{Failure, Success, Try}
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._

/** Defines PDND client assertion validations
  */
trait ClientAssertionValidation {

  final val jwtBearerClientAssertionType: String = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"
  final val clientCredentialsGrantType: String   = "client_credentials"

  /** Verifies if the client assertion type and grant type are the ones expected for PDND
    * @param clientAssertionType client assertion type
    * @param grantType grant type used for this assertion
    * @return
    */
  def validateAccessTokenRequest(clientAssertionType: String, grantType: String): Try[Unit] = {
    val result: Validated[NonEmptyList[Throwable], Unit] =
      (validateClientAssertionType(clientAssertionType), validateGrantType(grantType)).mapN((_: Unit, _: Unit) => ())

    result match {
      case Valid(unit) => Success(unit)
      case Invalid(e)  => Failure(InvalidAccessTokenRequest(e.map(_.getMessage).toList))
    }
  }

  private def validateClientAssertionType(clientAssertionType: String): ValidatedNel[Throwable, Unit] = {
    val validation = Either.cond(
      clientAssertionType == jwtBearerClientAssertionType,
      (),
      InvalidClientAssertionType(s"Client assertion type '$clientAssertionType' is not valid")
    )

    validation match {
      case Left(throwable) => throwable.invalidNel[Unit]
      case Right(_)        => ().validNel[Throwable]
    }

  }

  private def validateGrantType(grantType: String): ValidatedNel[Throwable, Unit] = {
    val validation = Either.cond(grantType == clientCredentialsGrantType, (), InvalidGrantType(grantType))

    validation match {
      case Left(throwable) => throwable.invalidNel[Unit]
      case Right(_)        => ().validNel[Throwable]
    }
  }

}
