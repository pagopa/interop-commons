package it.pagopa.interop.commons.mail

import courier.Defaults._
import courier._
import javax.mail.internet.InternetAddress
import scala.concurrent.Future

class InteropMailer private (sender: InternetAddress, mailer: Mailer) {

  def send(mail: Mail): Future[Unit] = sendMail(mail.recipients, mail.subject, renderMailContent(mail))

  private def renderMailContent(mail: Mail): Content = mail match {
    case TextMail(_, _, body, Nil) => Text(body)
    case TextMail(_, _, body, as)  =>
      as.foldLeft(Multipart().text(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
    case HttpMail(_, _, body, as)  =>
      as.foldLeft(Multipart().html(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
  }

  private def sendMail(recipients: Seq[InternetAddress], mailSubject: String, mailContent: Content) = mailer(
    Envelope
      .from(sender)
      .to(recipients: _*)
      .subject(mailSubject)
      .content(mailContent)
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
