package it.pagopa.interop.commons.signer.errors

final case object JWSAlgorithmNotFound extends Throwable(s"No JWSAlgorithm found for SignatureAlgorithm.Empty")
