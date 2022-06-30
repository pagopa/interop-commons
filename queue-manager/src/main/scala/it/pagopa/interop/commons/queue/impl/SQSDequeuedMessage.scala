package it.pagopa.interop.commons.queue.impl

final case class SQSDequeuedMessage[V](value: V, receiptHandle: String)
