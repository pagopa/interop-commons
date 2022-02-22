package it.pagopa.interop.commons.mail

import it.pagopa.interop.commons.mail.model.{MailAttachment, MailData, MailDataTemplate}
import it.pagopa.interop.commons.mail.service.InteropMailer
import it.pagopa.interop.commons.mail.service.impl.DefaultInteropMailer
import it.pagopa.interop.commons.utils.model.TextTemplate
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import javax.mail.internet.MimeMultipart

class MailerImplSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  val mailer: InteropMailer = new DefaultInteropMailer with MockMailerConfiguration

  "a MailSender" should {
    "send templated text email" in {

      val uuidToken = UUID.randomUUID()
      val party     = "Comune di Milano"

      val mailData = MailDataTemplate(
        recipients = Seq("legal@comune.milano.it"),
        subject = TextTemplate("Interop - Onboarding ${partyName}", Map("partyName" -> party)),
        body = TextTemplate("This is the UUID token ${token}", Map("token" -> uuidToken.toString)),
        attachments = Seq.empty
      )
      mailer.sendWithTemplate(mailData).futureValue

      val milanInbox = Mailbox.get("legal@comune.milano.it")
      milanInbox.size shouldBe 1
      val milanMsg = milanInbox.get(0)
      milanMsg.getContent shouldBe s"This is the UUID token ${uuidToken}"
      milanMsg.getSubject shouldBe "Interop - Onboarding Comune di Milano"
    }

    "send text email with attachments" in {
      val (tempFile, mimeType) = getTestResourceData("/Example.png")

      val mailData = MailData(
        recipients = Seq("legal@comune.bologna.it"),
        subject = "Interop - Onboarding",
        body = "Attachment ahead",
        attachments = Seq(MailAttachment("attachment", tempFile, mimeType))
      )
      mailer.send(mailData).futureValue

      val bolognaInbox = Mailbox.get("legal@comune.bologna.it")
      bolognaInbox.size shouldBe 1
      val bolognaMsg = bolognaInbox.get(0)
      bolognaMsg.getContent shouldBe a[MimeMultipart]
      bolognaMsg.getSubject shouldBe "Interop - Onboarding"
    }
  }

}
