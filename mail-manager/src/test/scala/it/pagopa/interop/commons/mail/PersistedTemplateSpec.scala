package it.pagopa.interop.commons.mail

import it.pagopa.interop.commons.mail.model.{PersistedTemplate, PersistedTemplateUnmarshaller}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Failure, Success}

class PersistedTemplateSpec extends AnyWordSpecLike with Matchers with PersistedTemplateUnmarshaller {

  "a string" should {
    "be converted to a proper persisted template" in {
      val templateStr =
        """
          |{
          |    "subject": "test",
          |    "body": "this is body"
          |}
          |""".stripMargin
      toPersistedTemplate(templateStr) shouldBe Success(PersistedTemplate(subject = "test", body = "this is body"))
    }

    "fail the conversion when invalid string is provided" in {
      val templateStr = """{"yada":"yada"}"""
      toPersistedTemplate(templateStr) shouldBe a[Failure[_]]
    }

    "be converted to a proper persisted template when base64 encoded" in {
      val templateStr =
        """
          |{
          |    "subject": "c3dlZGlzaCByb2NrcyAtIMOlIMOkIMO2",
          |    "body": "aW50ZXJvcCDDqMOow6jDqMOyw7LDssOgw7LDoMOyw6BhwqfDuQ==",
          |    "encoded": "true"
          |}
          |""".stripMargin
      toPersistedTemplate(templateStr) shouldBe Success(
        PersistedTemplate(subject = "swedish rocks - å ä ö", body = "interop èèèèòòòàòàòàa§ù")
      )
    }

    "fail if no encoding is in place when base64 encoded" in {
      val templateStr =
        """
          |{
          |    "subject": "hello this must fail",
          |    "body": "cGRuZCBpbnRlcm9wIMOow6jDqMOow7LDssOyw6DDssOgw7LDoGHCp8O5",
          |    "encoded": true
          |}
          |""".stripMargin
      toPersistedTemplate(templateStr) shouldBe a[Failure[_]]
    }
  }

}
