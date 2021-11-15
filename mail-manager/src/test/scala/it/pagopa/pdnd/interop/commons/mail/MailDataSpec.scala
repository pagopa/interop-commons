package it.pagopa.pdnd.interop.commons.mail

import it.pagopa.pdnd.interop.commons.mail.service.{MailAttachment, MailDataTemplate, TextTemplate}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File

class MailSpec extends AnyWordSpecLike with Matchers {

  "MailDataTemplate" should {

    "convert its content properly" in {
      val template = MailDataTemplate(
        recipients = Seq("mario@rossi.it"),
        subject = TextTemplate("this is a subject"),
        body = TextTemplate("this is a ${style} body", Map("style" -> "wonderful")),
        attachments = Seq(MailAttachment(new File("pippo.path")))
      )

      template.toMailData should equal(
        service.MailData(
          recipients = Seq("mario@rossi.it"),
          subject = "this is a subject",
          body = "this is a wonderful body",
          attachments = Seq(MailAttachment(new File("pippo.path")))
        )
      )
    }

  }

}
