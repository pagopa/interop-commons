package it.pagopa.interop.commons.mail

import javax.mail.internet.InternetAddress
import org.typelevel.literally.Literally
import scala.util.Try
import courier._

sealed trait Mail {
  val recipients: Seq[InternetAddress]
  val subject: String

  def renderContent: Content = this match {
    case TextMail(_, _, body, Nil) => Text(body)
    case TextMail(_, _, body, as)  =>
      as.foldLeft(Multipart().text(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
    case HttpMail(_, _, body, as)  =>
      as.foldLeft(Multipart().html(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
  }
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

  /**
    *
    * @param text comma separated emails list
    * @return
    */
  def from(text: String): Either[Throwable, List[InternetAddress]] =
    Try(InternetAddress.parse(text, true)).map(_.toList).toEither

  implicit class MailSyntax(val sc: StringContext) extends AnyVal {
    def mail(args: Any*): InternetAddress = macro MailLiteral.make
  }
}
