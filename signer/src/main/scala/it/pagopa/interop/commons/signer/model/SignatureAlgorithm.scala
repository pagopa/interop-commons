package it.pagopa.interop.commons.signer.model

sealed trait SignatureAlgorithm

object SignatureAlgorithm {

  case object RSAPssSha256 extends SignatureAlgorithm
  case object RSAPssSha384 extends SignatureAlgorithm
  case object RSAPssSha512 extends SignatureAlgorithm

  case object RSAPkcs1Sha256 extends SignatureAlgorithm
  case object RSAPkcs1Sha384 extends SignatureAlgorithm
  case object RSAPkcs1Sha512 extends SignatureAlgorithm

  case object ECSha256 extends SignatureAlgorithm
  case object ECSha384 extends SignatureAlgorithm
  case object ECSha512 extends SignatureAlgorithm

  case object Empty extends SignatureAlgorithm

}
