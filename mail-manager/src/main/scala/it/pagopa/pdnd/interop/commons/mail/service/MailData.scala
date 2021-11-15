package it.pagopa.pdnd.interop.commons.mail.service

import it.pagopa.pdnd.interop.commons.utils.TypeConversions.StringOps

import java.io.File

/** Models mail attachments
  * @param file attachment reference
  * @param name optional file name. If not defined, actual <code>file</code> name is used.
  */
final case class MailAttachment(file: File, name: Option[String] = None)

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

/** Models data required for sending e-mails with text templates instead of <code>String</code>s.<br>
  * This may be useful for clients that needs to dynamically customize either mail subject or body.
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
    MailData(recipients = recipients, subject = subject.toText, body = body.toText, attachments = attachments)
}

/** Defines a customizable text template through strings interpolation.<br>
  * Expected syntax for interpolation variables is: <code>${variable}</code>
  * @param text string representing a text template
  * @param variables variables to be replaced in the template, empty by default
  */
case class TextTemplate(text: String, variables: Map[String, String] = Map.empty) {
  def toText: String = text interpolate variables
}
