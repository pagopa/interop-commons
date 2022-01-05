package it.pagopa.pdnd.interop.commons.utils.errors

final case class ResourceConflictError(resourceId: String)
    extends ComponentError("9998", s"Resource $resourceId already exists")
