package it.pagopa.interop.commons.queue.config

final case class SQSHandlerConfig(
  queueUrl: String,
  maxConcurrency: Int = 50,
  visibilityTimeout: Int,
  messageGroupId: Option[String]
)
