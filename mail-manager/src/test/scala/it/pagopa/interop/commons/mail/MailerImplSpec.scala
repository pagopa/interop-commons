package it.pagopa.interop.commons.mail

import org.jvnet.mock_javamail.Mailbox
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import Mail._
import javax.mail.internet.InternetAddress
import javax.mail.Message
import javax.mail.internet.MimeMultipart

class MailerImplSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  val mailer: InteropMailer = InteropMailer.unsafeFrom(mail"mock-sender@interop.pagopa.it", Mocks.mockedMailer())

  val timeout: Timeout = Timeout(Span(3, Seconds))

  "a MailSender" should {
    "send a text email" in {
      val mail: Mail = TextMail(
        recipients = Seq(mail"foo@comune.furlocchio.it"),
        subject = "Hello World",
        body = "That's the mail body",
        attachments = Seq.empty
      )

      mailer.send(mail).futureValue(timeout)

      val milanInbox: Mailbox = Mailbox.get("foo@comune.furlocchio.it")
      milanInbox.size shouldBe 1
      val milanMsg: Message   = milanInbox.get(0)
      milanMsg.getContent shouldBe s"That's the mail body"
      milanMsg.getSubject shouldBe "Hello World"
    }

    "send text email with attachments" in {
      val bytes: Array[Byte] = getClass.getResourceAsStream("/Example.png").readAllBytes()

      val mailData = TextMail(
        recipients = Seq(mail"bar@comune.snasalino.it"),
        subject = "Interop ciao ciao",
        body = "Ci sono allegati",
        attachments = Seq(MailAttachment("attachment", bytes, "image/png"))
      )
      mailer.send(mailData).futureValue(timeout)

      val bolognaInbox = Mailbox.get("bar@comune.snasalino.it")
      bolognaInbox.size shouldBe 1
      val bolognaMsg   = bolognaInbox.get(0)
      bolognaMsg.getSubject shouldBe "Interop ciao ciao"
      bolognaMsg.getContent shouldBe a[MimeMultipart]

      val multipart: MimeMultipart = bolognaMsg.getContent().asInstanceOf[MimeMultipart]
      multipart.getCount() shouldBe 2
      multipart.getBodyPart(0).getContent().asInstanceOf[String] shouldBe "Ci sono allegati"
      multipart.getBodyPart(1).getFileName() shouldBe "attachment"
      multipart.getBodyPart(1).getContentType() shouldBe "image/png; name=attachment"
    }
  }

}
