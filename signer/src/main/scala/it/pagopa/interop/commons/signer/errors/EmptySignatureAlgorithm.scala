package it.pagopa.interop.commons.signer.errors

final case object EmptySignatureAlgorithmError extends Throwable(s"No signature algorithm has been passed")
