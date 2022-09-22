package it.pagopa.interop.commons.ratelimiter.error

object Errors {
  case object TooManyRequests                         extends Throwable("Too many requests")
  final case class DeserializationFailed(key: String) extends Throwable(s"Deserialization failed for key $key")
}
