package it.pagopa.pdnd.interop.commons.mail

import it.pagopa.pdnd.interop.commons.mail.model.{PersistedTemplate, PersistedTemplateUnmarshaller}
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

    ", as base64, be converted to a proper persisted template" in {
      val templateStr =
        """
          |{
          |    "subject": "c3dlZGlzaCByb2NrcyAtIMOlIMOkIMO2",
          |    "body": "cGRuZCBpbnRlcm9wIMOow6jDqMOow7LDssOyw6DDssOgw7LDoGHCp8O5",
          |    "encoded": "true"
          |}
          |""".stripMargin
      toPersistedTemplate(templateStr) shouldBe Success(
        PersistedTemplate(subject = "swedish rocks - å ä ö", body = "pdnd interop èèèèòòòàòàòàa§ù")
      )
    }

    ", as base64, fail if no encoding is in place" in {
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
