package it.pagopa.interop.commons.mail

import courier.Defaults._
import courier._
import javax.mail.internet.InternetAddress
import scala.concurrent.Future

class InteropMailer private (sender: InternetAddress, mailer: Mailer) {

  def send(mail: Mail): Future[Unit] = mailer(
    Envelope
      .from(sender)
      .to(mail.recipients: _*)
      .subject(mail.subject)
      .content(mail.renderContent)
  )
}

object InteropMailer {
  def from(config: MailConfiguration): InteropMailer = new InteropMailer(
    config.sender,
    Mailer(config.smtp.serverAddress, config.smtp.serverPort)
      .auth(config.smtp.authenticated.getOrElse(true))
      .ssl(config.smtp.withSsl.getOrElse(true))
      .as(config.smtp.user, config.smtp.password)
      .startTls(config.smtp.withTls.getOrElse(true))()
  )

  def unsafeFrom(sender: InternetAddress, mailer: Mailer): InteropMailer = new InteropMailer(sender, mailer)
}
