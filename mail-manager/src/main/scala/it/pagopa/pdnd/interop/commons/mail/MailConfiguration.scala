package it.pagopa.pdnd.interop.commons.mail

import com.typesafe.config.{Config, ConfigFactory}

case class MailConfigurationInfo(senderAddress: String, smtp: SMTPConfiguration)
case class SMTPConfiguration(
  user: String,
  password: String,
  serverAddress: String,
  serverPort: Integer,
  authenticated: Boolean,
  withTls: Boolean
)

object MailConfiguration {
  lazy val config: Config = ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference())

  val configuration =
    MailConfigurationInfo(
      senderAddress = config.getString("pdnd-interop-commons.mail.sender"),
      smtp = SMTPConfiguration(
        user = config.getString("pdnd-interop-commons.mail.smtp.user"),
        password = config.getString("pdnd-interop-commons.mail.smtp.password"),
        serverAddress = config.getString("pdnd-interop-commons.mail.smtp.server"),
        serverPort = config.getInt("pdnd-interop-commons.mail.smtp.port"),
        authenticated = config.getBoolean("pdnd-interop-commons.mail.smtp.authenticated"),
        withTls = config.getBoolean("pdnd-interop-commons.mail.smtp.with-tls")
      )
    )

}
