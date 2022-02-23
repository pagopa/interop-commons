package it.pagopa.interop.commons.mail.service

import courier.Mailer
import it.pagopa.interop.commons.mail.model.{MailData, MailDataTemplate}

import javax.mail.internet.InternetAddress
import scala.concurrent.Future

/** Defines common operations for e-mail delivery
  */
trait InteropMailer {

  /** Sends an e-mail
    * @param mailData data containing all the mail information
    * @return
    */
  def send(mailData: MailData): Future[Unit]

  /** Sends an e-mail built with text templates. This may be useful for clients needing to dynamically customize mail either mail subject or body
    * @param mailDataTemplate mail data with customizable text body
    * @return
    */
  def sendWithTemplate(mailDataTemplate: MailDataTemplate): Future[Unit] =
    send(mailDataTemplate.toMailData)
}

trait MailerInstance {
  val mailer: Mailer
  val sender: InternetAddress
}
