package it.pagopa.pdnd.interop.commons.mail.service

import java.io.File
import scala.concurrent.Future

/** Defines common operations for e-mail delivery
  */

//TODO complete this
trait Mailer {

  /** Sends an e-mail with an attachment
    * @param recipients - e-mail recipients collection
    * @param token
    * @param attachment - file attached to e-mail
    * @return
    */
  def sendWithAttachment(recipients: Seq[String], token: String, attachment: File): Future[Unit]

  /** Sends an e-mail
    * @param recipients - e-mail recipients collection
    * @param token
    * @return
    */
  def send(recipients: Seq[String], token: String): Future[Unit]
}
