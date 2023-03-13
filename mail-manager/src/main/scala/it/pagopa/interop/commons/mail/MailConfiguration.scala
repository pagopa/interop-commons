package it.pagopa.interop.commons.mail

import pureconfig._
import javax.mail.internet.InternetAddress
import scala.util.Try

final case class MailConfiguration(sender: InternetAddress, smtp: SMTPConfiguration)

final case class SMTPConfiguration(
  user: String,
  password: String,
  serverAddress: String,
  serverPort: Int,
  authenticated: Option[Boolean],
  withTls: Option[Boolean],
  withSsl: Option[Boolean]
)

object MailConfiguration {
  implicit val inetReader: ConfigReader[InternetAddress] = ConfigReader.fromStringTry(s => Try(new InternetAddress(s)))
}
