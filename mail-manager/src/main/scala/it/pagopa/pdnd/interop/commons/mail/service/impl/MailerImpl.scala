package it.pagopa.pdnd.interop.commons.mail.service.impl

import courier.Defaults._
import courier._
import it.pagopa.pdnd.interop.commons.mail.model.MailData
import it.pagopa.pdnd.interop.commons.mail.service.{MailerInstance, PDNDMailer}

import javax.mail.internet.InternetAddress
import scala.concurrent.Future
import scala.util.Try

trait MailerImpl extends PDNDMailer { mailerInstance: MailerInstance =>

  override def send(mailData: MailData): Future[Unit] = {
    val mailContent = mailData.attachments match {
      case Nil => Text(mailData.body)
      case _   => buildMultipart(mailData)
    }

    sendMail(mailData.recipients, mailData.subject, mailContent)
  }

  private def buildMultipart(mailData: MailData) = {
    val attachments     = mailData.attachments.iterator
    val multipartObject = Multipart()

    while (attachments.hasNext) {
      val file = attachments.next()
      multipartObject.attach(file.file, file.name)
    }

    multipartObject.html(mailData.body)
  }

  private def sendMail(recipients: Seq[String], mailSubject: String, mailContent: Content) = {
    val parsedEmails: Future[Seq[InternetAddress]] = Future.traverse(recipients)(parseRecipientAddress)
    parsedEmails.flatMap { to =>
      val sender = mailerInstance.mailer
      sender(
        Envelope
          .from(mailerInstance.sender)
          .to(to: _*)
          .subject(mailSubject)
          .content(mailContent)
      )
    }
  }

  private def parseRecipientAddress(address: String) = Future.fromTry(Try(address.addr))

}
