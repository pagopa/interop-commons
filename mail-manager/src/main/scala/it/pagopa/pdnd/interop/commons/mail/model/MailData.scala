package it.pagopa.pdnd.interop.commons.mail.model

/** Models data required for sending e-mails
  * @param recipients e-mail recipient list
  * @param subject e-mail subject
  * @param body e-mail body
  * @param attachments e-mail attachments, empty by default
  */
final case class MailData(
  recipients: Seq[String],
  subject: String,
  body: String,
  attachments: Seq[MailAttachment] = Seq.empty
)
