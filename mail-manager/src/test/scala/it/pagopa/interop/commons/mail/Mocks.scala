package it.pagopa.interop.commons.mail

import courier.Mailer
import org.jvnet.mock_javamail.MockTransport

import java.util.Properties
import javax.mail.{Provider, Session}

object Mocks {

  class MockedSMTPProvider
      extends Provider(Provider.Type.TRANSPORT, "mocked", classOf[MockTransport].getName, "Mock", null)

  def mockedMailer(): Mailer = {
    val properties: Properties = new Properties
    properties.put("mail.transport.protocol.rfc822", "mocked")
    val mockedSession: Session = Session.getDefaultInstance(properties)
    mockedSession.setProvider(new MockedSMTPProvider)
    Mailer(mockedSession)
  }
}
