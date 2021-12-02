package it.pagopa.pdnd.interop.commons.mail.model

import it.pagopa.pdnd.interop.commons.mail.model
import it.pagopa.pdnd.interop.commons.utils.model.TextTemplate

/** Models data required for sending e-mails with text templates instead of <code>String</code>s.<br>
  * This may be useful for clients that needs to dynamically customize either mail subject or body.
  *
  * @param recipients e-mail recipient list
  * @param subject e-mail subject text template
  * @param body e-mail body text template
  * @param attachments e-mail attachments, empty by default
  */
final case class MailDataTemplate(
  recipients: Seq[String],
  subject: TextTemplate,
  body: TextTemplate,
  attachments: Seq[MailAttachment] = Seq.empty
) {

  def toMailData: MailData =
    model.MailData(recipients = recipients, subject = subject.toText, body = body.toText, attachments = attachments)
}
