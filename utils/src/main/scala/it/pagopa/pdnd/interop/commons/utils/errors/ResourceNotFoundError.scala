package it.pagopa.pdnd.interop.commons.utils.errors

final case class ResourceNotFoundError(resourceId: String)
    extends ComponentError("9997", s"Resource $resourceId not found")
