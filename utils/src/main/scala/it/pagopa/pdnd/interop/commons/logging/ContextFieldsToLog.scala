package it.pagopa.pdnd.interop.commons.logging

final case class ContextFieldsToLog(correlationId: Option[String] = None, userId: Option[String] = None)
