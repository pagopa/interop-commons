package it.pagopa.pdnd.interop.commons.mail

import it.pagopa.pdnd.interop.commons.mail.model.{MailAttachment, MailData, MailDataTemplate, TextTemplate}
import it.pagopa.pdnd.interop.commons.mail.service._
import it.pagopa.pdnd.interop.commons.mail.service.impl.DefaultPDNDMailer
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import javax.mail.internet.MimeMultipart

class MailerImplSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  val mailer: PDNDMailer = new DefaultPDNDMailer with MockMailerConfiguration

  "a MailSender" should {
    "send templated text email" in {

      val uuidToken = UUID.randomUUID()
      val party     = "Comune di Milano"

      val mailData = MailDataTemplate(
        recipients = Seq("legal@comune.milano.it"),
        subject = TextTemplate("PDND Interop - Onboarding ${partyName}", Map("partyName" -> party)),
        body = TextTemplate("This is the UUID token ${token}", Map("token" -> uuidToken.toString)),
        attachments = Seq.empty
      )
      mailer.sendWithTemplate(mailData).futureValue

      val milanInbox = Mailbox.get("legal@comune.milano.it")
      milanInbox.size shouldBe 1
      val milanMsg = milanInbox.get(0)
      milanMsg.getContent shouldBe s"This is the UUID token ${uuidToken}"
      milanMsg.getSubject shouldBe "PDND Interop - Onboarding Comune di Milano"
    }

    "send text email with attachments" in {
      val (tempFile, mimeType) = getTestResourceData("/Example.png")

      val mailData = MailData(
        recipients = Seq("legal@comune.bologna.it"),
        subject = "PDND Interop - Onboarding",
        body = "Attachment ahead",
        attachments = Seq(MailAttachment("attachment", tempFile, mimeType))
      )
      mailer.send(mailData).futureValue

      val bolognaInbox = Mailbox.get("legal@comune.bologna.it")
      bolognaInbox.size shouldBe 1
      val bolognaMsg = bolognaInbox.get(0)
      bolognaMsg.getContent shouldBe a[MimeMultipart]
      bolognaMsg.getSubject shouldBe "PDND Interop - Onboarding"
    }
  }

}
