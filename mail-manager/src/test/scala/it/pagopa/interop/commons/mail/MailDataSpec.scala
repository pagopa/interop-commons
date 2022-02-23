package it.pagopa.interop.commons.mail

import it.pagopa.interop.commons.mail.model.{MailAttachment, MailData, MailDataTemplate}
import it.pagopa.interop.commons.utils.model.TextTemplate
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MailSpec extends AnyWordSpecLike with Matchers {

  "MailDataTemplate" should {

    "convert its content properly" in {
      val (tempFile, mimeType) = getTestResourceData("/Example.png")
      val attachment           = MailAttachment("attachmentname", tempFile, mimeType)

      val template = MailDataTemplate(
        recipients = Seq("mario@rossi.it"),
        subject = TextTemplate("this is a subject"),
        body = TextTemplate("this is a ${style} body", Map("style" -> "wonderful")),
        attachments = Seq(attachment)
      )

      template.toMailData should equal(
        MailData(
          recipients = Seq("mario@rossi.it"),
          subject = "this is a subject",
          body = "this is a wonderful body",
          attachments = Seq(attachment)
        )
      )
    }

  }

}
