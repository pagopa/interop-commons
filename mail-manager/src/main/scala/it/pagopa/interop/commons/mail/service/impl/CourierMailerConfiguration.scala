package it.pagopa.interop.commons.mail.service.impl

import courier._
import it.pagopa.interop.commons.mail.MailConfiguration.config
import it.pagopa.interop.commons.mail.service.MailerInstance

import javax.mail.internet.InternetAddress

/** Defines configuration for a Courier Mailer implementation
  */
object CourierMailerConfiguration {

  lazy val mailerConfig = Mailer(config.smtp.serverAddress, config.smtp.serverPort)
    .auth(config.smtp.authenticated)
    .ssl(config.smtp.ssl)
    .as(config.smtp.user, config.smtp.password)
    .startTls(config.smtp.withTls)()

  /** Defines DI instance for a mailer instance using Courier configuration
    */
  trait CourierMailer extends MailerInstance {
    override val mailer: Mailer          = mailerConfig
    override val sender: InternetAddress = config.senderAddress.addr
  }
}
