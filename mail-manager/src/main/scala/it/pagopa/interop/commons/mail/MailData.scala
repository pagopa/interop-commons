package it.pagopa.interop.commons.mail

import javax.mail.internet.InternetAddress
import org.typelevel.literally.Literally
import scala.util.Try

sealed trait Mail {
  val recipients: Seq[InternetAddress]
  val subject: String
  val body: String
  val attachments: Seq[MailAttachment]
}

final case class TextMail(
  recipients: Seq[InternetAddress],
  subject: String,
  body: String,
  attachments: Seq[MailAttachment] = Seq.empty
) extends Mail

final case class HttpMail(
  recipients: Seq[InternetAddress],
  subject: String,
  body: String,
  attachments: Seq[MailAttachment] = Seq.empty
) extends Mail

final case class MailAttachment(name: String, bytes: Array[Byte], mimeType: String)

object Mail {
  private object MailLiteral extends Literally[InternetAddress] {
    def validate(c: Context)(s: String): Either[String, c.Expr[InternetAddress]] = {
      import c.universe.{Try => _, _}
      Try(InternetAddress.parse(s, true)).toOption match {
        case None    => Left(s"Invalid mail")
        case Some(_) => Right(c.Expr(q"new InternetAddress($s)"))
      }
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[InternetAddress] = apply(c)(args: _*)
  }

  implicit class MailSyntax(val sc: StringContext) extends AnyVal {
    def mail(args: Any*): InternetAddress = macro MailLiteral.make
  }
}
