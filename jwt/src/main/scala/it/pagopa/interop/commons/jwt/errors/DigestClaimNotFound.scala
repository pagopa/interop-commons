package it.pagopa.interop.commons.jwt.errors

final case class DigestClaimNotFound(claim: String) extends Throwable(s"Digest claim $claim not found")
