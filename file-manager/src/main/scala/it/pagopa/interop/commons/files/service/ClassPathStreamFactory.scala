package it.pagopa.interop.commons.files.service

import com.openhtmltopdf.extend.{FSStream, FSStreamFactory}

import java.net.URI

class ClassPathStreamFactory extends FSStreamFactory {
  override def getUrl(uri: String): FSStream = {
    val fullUri = new URI(uri)
    new ClassPathStream(fullUri.getPath)
  }
}
