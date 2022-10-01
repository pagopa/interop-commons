package it.pagopa.interop.commons.utils.errors

object GenericComponentErrors {
  final case object MissingBearer extends ComponentError("9999", "Bearer token has not been passed")

  final case class ResourceConflictError(resourceId: String)
      extends ComponentError("9998", s"Resource $resourceId already exists")

  final case class ResourceNotFoundError(resourceId: String)
      extends ComponentError("9997", s"Resource $resourceId not found")

  final case object MissingUserId extends ComponentError("9996", "Uid has not been passed")

  final case object MissingSub extends ComponentError("9995", "Subject has not been passed")

  final case class MissingHeader(headerName: String)
      extends ComponentError("9994", s"Header $headerName not existing in this request")

  final case class GenericClientError(errorMessage: String) extends ComponentError("9993", errorMessage)

  final case class ThirdPartyCallError(serviceName: String, errorMessage: String)
      extends ComponentError("9992", s"Error while invoking $serviceName external service -> $errorMessage")

  final case class GenericError(errorMessage: String) extends ComponentError("9991", errorMessage)

  final case class MissingClaim(claimName: String)
      extends ComponentError("9990", s"Claim $claimName has not been passed")

  case object OperationForbidden extends ComponentError("9989", "Insufficient privileges")

  case object TooManyRequests extends ComponentError("9988", s"Too many requests")

  final case class ValidationRequestError(validationErrorMessage: String)
      extends ComponentError("9000", validationErrorMessage)

}
