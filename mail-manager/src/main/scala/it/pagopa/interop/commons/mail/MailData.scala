package it.pagopa.interop.commons.mail

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.json._
import io.circe._
import io.circe.generic.semiauto._
import javax.mail.internet.InternetAddress
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.typelevel.literally.Literally
import scala.util.Try
import courier._
import java.util.UUID

sealed trait Mail {
  val id: UUID
  val recipients: Seq[InternetAddress]
  val subject: String

  def renderContent: Content = this match {
    case TextMail(_, _, _, body, Nil) => Text(body)
    case TextMail(_, _, _, body, as)  =>
      as.foldLeft(Multipart().text(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
    case HttpMail(_, _, _, body, as)  =>
      as.foldLeft(Multipart().html(body)) { (c, a) => c.attachBytes(a.bytes, a.name, a.mimeType) }
  }
}

final case class TextMail(
  id: UUID = UUID.randomUUID(),
  recipients: Seq[InternetAddress],
  subject: String,
  body: String,
  attachments: Seq[MailAttachment] = Seq.empty
) extends Mail

final case class HttpMail(
  id: UUID = UUID.randomUUID(),
  recipients: Seq[InternetAddress],
  subject: String,
  body: String,
  attachments: Seq[MailAttachment] = Seq.empty
) extends Mail

final case class MailAttachment(name: String, bytes: Array[Byte], mimeType: String) {
  override def equals(obj: Any): Boolean = obj match {
    case MailAttachment(n, b, m) => name == n && m == mimeType && bytes.sameElements(b)
    case _                       => false
  }
}

object Mail extends SprayJsonSupport with DefaultJsonProtocol {

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
    * Convert a comma-separated email text to a list of InternetAddress
    *
    * Usage:
    * {{{
    *   val commaSeparatedEmails: String = "a@mail.com,b@mail.com,c@mail.com"
    *   val emails: Either[Throwable, List[InternetAddress]] = addresses(commaSeparatedEmails)
    * }}}
    */
  def addresses(text: String): Either[Throwable, List[InternetAddress]] =
    Try(InternetAddress.parse(text, true)).map(_.toList).toEither

  implicit class MailSyntax(val sc: StringContext) extends AnyVal {
    def mail(args: Any*): InternetAddress = macro MailLiteral.make
  }

  implicit val encodeInternetAddress: Encoder[InternetAddress] =
    Encoder.encodeString.contramap[InternetAddress](_.getAddress())

  implicit val decodeInstant: Decoder[InternetAddress] = Decoder.decodeString.emapTry { str =>
    Try(new InternetAddress(str))
  }

  implicit val mailAttachmentEncoder: Encoder[MailAttachment] = deriveEncoder
  implicit val mailAttachmentDecoder: Decoder[MailAttachment] = deriveDecoder

  implicit val textMailEncoder: Encoder[TextMail] = deriveEncoder
  implicit val textMailDecoder: Decoder[TextMail] = deriveDecoder

  implicit val httpMailEncoder: Encoder[HttpMail] = deriveEncoder
  implicit val httpMailDecoder: Decoder[HttpMail] = deriveDecoder

  implicit val mailAttachmentFormat: RootJsonFormat[MailAttachment] = jsonFormat3(MailAttachment)

  implicit object InternetAddressFormat extends JsonFormat[InternetAddress] {
    def write(ia: InternetAddress) = JsString(s"${ia.getAddress()}")
    def read(json: JsValue)        = json match {
      case JsString(address) => new InternetAddress(address)
      case _                 => deserializationError("JString type expected")
    }
  }
  implicit val textMailFormat: RootJsonFormat[TextMail] = jsonFormat5(TextMail)
  implicit val httpMailFormat: RootJsonFormat[HttpMail] = jsonFormat5(HttpMail)
}
