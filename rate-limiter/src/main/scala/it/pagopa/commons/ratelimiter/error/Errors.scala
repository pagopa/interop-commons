package it.pagopa.commons.ratelimiter.error

object Errors {
  case object TooManyRequests extends Throwable("Too many requests")
}
