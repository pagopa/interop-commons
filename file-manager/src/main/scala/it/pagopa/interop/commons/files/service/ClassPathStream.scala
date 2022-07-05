package it.pagopa.interop.commons.files.service

import com.openhtmltopdf.extend.FSStream

import java.io.{InputStream, InputStreamReader, Reader}
import java.nio.charset.StandardCharsets

class ClassPathStream(val uri: String) extends FSStream {
  override def getStream: InputStream = getClass.getResourceAsStream(uri)

  override def getReader: Reader = new InputStreamReader(getClass.getResourceAsStream(uri), StandardCharsets.UTF_8)
}
