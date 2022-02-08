package it.pagopa.pdnd.interop.commons.utils.errors

object GenericComponentErrors {
  final case object MissingBearer extends ComponentError("9999", "Bearer token has not been passed")

  final case class ResourceConflictError(resourceId: String)
      extends ComponentError("9998", s"Resource $resourceId already exists")

  final case class ResourceNotFoundError(resourceId: String)
      extends ComponentError("9997", s"Resource $resourceId not found")

  final case object MissingUserId extends ComponentError("9996", "Uid has not been passed")

  final case object MissingSub extends ComponentError("9995", "Subject has not been passed")

  final case class MissingClaim(claimName: String)
      extends ComponentError("9990", s"Claim $claimName has not been passed")

  final case class ValidationRequestError(errorMessage: String) extends ComponentError("9000", errorMessage)

}
