package it.pagopa.pdnd.interop.commons

import courier.Mailer
import it.pagopa.pdnd.interop.commons.mail.service.MailerInstance
import org.jvnet.mock_javamail.MockTransport

import java.util.Properties
import javax.mail.Provider
import javax.mail.internet.InternetAddress

package object mail {

  private[mail] class MockedSMTPProvider
      extends Provider(Provider.Type.TRANSPORT, "mocked", classOf[MockTransport].getName, "Mock", null)

  private[mail] object MockedMailerSession {
    val mockedSession = javax.mail.Session.getDefaultInstance(new Properties() {
      {
        put("mail.transport.protocol.rfc822", "mocked")
      }
    })
    mockedSession.setProvider(new MockedSMTPProvider)
  }

  private[mail] trait MockMailerConfiguration extends MailerInstance {
    override val mailer: Mailer          = Mailer(MockedMailerSession.mockedSession)
    override val sender: InternetAddress = new InternetAddress("pdnd-mock@pagopa.it")
  }

}
