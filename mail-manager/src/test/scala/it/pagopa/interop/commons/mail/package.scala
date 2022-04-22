package it.pagopa.interop.commons

import courier.Mailer
import it.pagopa.interop.commons.mail.service.MailerInstance
import org.jvnet.mock_javamail.MockTransport

import java.io.File
import java.nio.file.Files
import java.util.Properties
import javax.mail.Provider
import javax.mail.internet.InternetAddress

package object mail {

  // Mock SMTP provider
  private[mail] class MockedSMTPProvider
      extends Provider(Provider.Type.TRANSPORT, "mocked", classOf[MockTransport].getName, "Mock", null)

  // Mocks mails
  private[mail] object MockedMailerSession {
    val mockedSession = javax.mail.Session.getDefaultInstance(new Properties() {
      {
        put("mail.transport.protocol.rfc822", "mocked")
      }
    })
    mockedSession.setProvider(new MockedSMTPProvider)
  }

  // mock SMTP server setup
  private[mail] trait MockMailerConfiguration extends MailerInstance {
    override val mailer: Mailer          = Mailer(MockedMailerSession.mockedSession)
    override val sender: InternetAddress = new InternetAddress("mock-sender@interop.pagopa.it")
  }

  def getTestResourceData(resource: String): (Array[Byte], String) = {
    val path     = getClass.getResource(resource).getPath
    val filePath = new File(path).toPath
    val content  = Files.readAllBytes(filePath)
    val mimeType = Files.probeContentType(filePath)
    (content, mimeType)
  }
}
