package it.pagopa.interop.commons.mail

import courier.Defaults._
import courier._
import javax.mail.internet.InternetAddress
import scala.concurrent.Future

class InteropMailer private (sender: InternetAddress, mailer: Mailer) {

  def send(mailData: Mail): Future[Unit] = sendMail(
    mailData.recipients,
    mailData.subject,
    mailData.attachments match {
      case Nil => Text(mailData.body)
      case xs  =>
        xs.foldLeft(Multipart().text(mailData.body)) { (mailContent, attachment) =>
          mailContent.attachBytes(attachment.bytes, attachment.name, attachment.mimeType)
        }
    }
  )

  private def sendMail(recipients: Seq[InternetAddress], mailSubject: String, mailContent: Content) = mailer(
    Envelope
      .from(sender)
      .to(recipients: _*)
      .subject(mailSubject)
      .content(mailContent)
  )
}

object InteropMailer {
  def create(): Either[Throwable, InteropMailer] = MailConfiguration.read().map { config =>
    val mailer: Mailer = Mailer(config.smtp.serverAddress, config.smtp.serverPort)
      .auth(config.smtp.authenticated.getOrElse(true))
      .ssl(config.smtp.withSsl.getOrElse(true))
      .as(config.smtp.user, config.smtp.password)
      .startTls(config.smtp.withTls.getOrElse(true))()
    new InteropMailer(config.sender, mailer)
  }

  def unsafeFrom(sender: InternetAddress, mailer: Mailer): InteropMailer = new InteropMailer(sender, mailer)
}
