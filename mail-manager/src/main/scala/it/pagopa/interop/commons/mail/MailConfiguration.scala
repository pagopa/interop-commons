package it.pagopa.interop.commons.mail

import com.typesafe.config.{Config, ConfigFactory}

/** Models mail configuration data
  * @param senderAddress address of the mail sender
  * @param smtp SMTP configuration data
  */
case class MailConfigurationInfo(senderAddress: String, smtp: SMTPConfiguration)

/** Models SMTP configuration data
  * @param user SMTP user
  * @param password SMTP password
  * @param serverAddress SMTP server address
  * @param serverPort SMTP server port
  * @param authenticated flag for making SMTP authentication required (default <code>true</code>)
  * @param withTls flag for making SMTP working with TLS (default <code>true</code>)
  */
case class SMTPConfiguration(
  user: String,
  password: String,
  serverAddress: String,
  serverPort: Integer,
  authenticated: Boolean,
  withTls: Boolean,
  ssl: Boolean
)

/** Defines mail manager configuration
  */
object MailConfiguration {
  lazy val hoconConfig: Config =
    ConfigFactory.defaultApplication().withFallback(ConfigFactory.defaultReference()).resolve()

  /** Returns currently mail-manager configuration data
    */
  val config =
    MailConfigurationInfo(
      senderAddress = hoconConfig.getString("interop-commons.mail.sender"),
      smtp = SMTPConfiguration(
        user = hoconConfig.getString("interop-commons.mail.smtp.user"),
        password = hoconConfig.getString("interop-commons.mail.smtp.password"),
        serverAddress = hoconConfig.getString("interop-commons.mail.smtp.server"),
        serverPort = hoconConfig.getInt("interop-commons.mail.smtp.port"),
        authenticated = hoconConfig.getBoolean("interop-commons.mail.smtp.authenticated"),
        withTls = hoconConfig.getBoolean("interop-commons.mail.smtp.with-tls"),
        ssl = hoconConfig.getBoolean("interop-commons.mail.smtp.ssl")
      )
    )
}
