package logging

final case class ContextFieldsToLog(correlationId: Option[String], userId: Option[String])
