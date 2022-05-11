package it.pagopa.interop.commons.jwt.model

sealed trait JWTAlgorithmType {
  def signatureAlgorithm: Option[String]
}

final case object RSA extends JWTAlgorithmType {
  override def signatureAlgorithm: Option[String] = Some("pkcs1v15")
}
final case object EC  extends JWTAlgorithmType {
  override def signatureAlgorithm: Option[String] = None
}
