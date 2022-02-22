package it.pagopa.interop.commons.mail.model

/** Models mail attachments
  *
  * @param file attachment reference
  * @param name attachment name.
  */
final case class MailAttachment(name: String, bytes: Array[Byte], mimetype: String)
