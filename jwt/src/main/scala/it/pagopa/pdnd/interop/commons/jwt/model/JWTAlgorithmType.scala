package it.pagopa.pdnd.interop.commons.jwt.model

sealed trait JWTAlgorithmType

final case object RSA extends JWTAlgorithmType
final case object EC  extends JWTAlgorithmType
