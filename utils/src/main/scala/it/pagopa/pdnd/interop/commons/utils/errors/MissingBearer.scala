package it.pagopa.pdnd.interop.commons.utils.errors

final case object MissingBearer extends ComponentError("9999", "Bearer token has not been passed")
