package it.pagopa.pdnd.interop.commons.mail.model

/** Models a persisted version of a mail
  * @param subject subject of the mail
  * @param body template of the mail
  */
final case class PersistedTemplate(subject: String, body: String)
