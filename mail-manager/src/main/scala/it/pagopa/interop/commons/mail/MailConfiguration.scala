package it.pagopa.interop.commons.mail

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import javax.mail.internet.InternetAddress
import scala.util.Try

final case class MailConfiguration(sender: InternetAddress, smtp: SMTPConfiguration)

final case class SMTPConfiguration(
  user: String,
  password: String,
  serverAddress: String,
  serverPort: Int,
  authenticated: Boolean,
  withTls: Boolean,
  ssl: Boolean
)

object MailConfiguration {
  implicit val inetReader: ConfigReader[InternetAddress] =
    ConfigReader.fromStringTry(s => Try(new InternetAddress(s)))

  def read(): Either[Throwable, MailConfiguration] = ConfigSource.file("mailer").load[MailConfiguration] match {
    case Left(errs)    => Left(new ConfigReaderException(errs))
    case Right(config) => Right(config)
  }
}
