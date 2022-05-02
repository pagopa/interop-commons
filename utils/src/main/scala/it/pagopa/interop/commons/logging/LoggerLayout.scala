package it.pagopa.interop.commons.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants.LINE_SEPARATOR
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.util.CachingDateFormatter
import ch.qos.logback.classic.Level

final class LoggerLayout extends LayoutBase[ILoggingEvent] {

  val cachingDateFormatter = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss.SSS")

  override def doLayout(event: ILoggingEvent): String = {
    val sbuf: StringBuffer = new StringBuffer(128)
    val time: String       = cachingDateFormatter.format(event.getTimeStamp)
    val name: String       = buildinfo.BuildInfo.name
    val level: String      = event.getLevel.toString()
    val loggerName: String = event.getLoggerName
    val message: String    = event.getFormattedMessage()

    sbuf.append(s"$time [$name] $level [$loggerName] - $message")
    sbuf.append(LINE_SEPARATOR)
    sbuf.toString()
  }

}
