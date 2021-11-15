package it.pagopa.pdnd.interop.commons.mail.model

import java.io.File

/** Models mail attachments
  *
  * @param file attachment reference
  * @param name optional file name. If not defined, actual <code>file</code> name is used.
  */
final case class MailAttachment(file: File, name: Option[String] = None)
