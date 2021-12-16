package logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.util.CachingDateFormatter

/** Defines the Logback Pattern Layout for PDND Interop
  */
final class LoggerLayout extends PatternLayout {

  val cachingDateFormatter = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss.SSS")
  final val EMPTY_SPACE    = " "

  override def doLayout(event: ILoggingEvent): String = {
    val sbuf = new StringBuffer(128)
    sbuf.append(cachingDateFormatter.format(event.getTimeStamp))
    sbuf.append(EMPTY_SPACE)
    sbuf.append(withinBrackets(buildinfo.BuildInfo.name))
    sbuf.append(EMPTY_SPACE)
    sbuf.append(event.getLevel)
    sbuf.append(EMPTY_SPACE)
    sbuf.append(withinBrackets(event.getThreadName))
    sbuf.append(EMPTY_SPACE)
    sbuf.append(withinBrackets(event.getLoggerName))
    sbuf.append(" - ")
    sbuf.append(event.getFormattedMessage())
    sbuf.append(CoreConstants.LINE_SEPARATOR)
    sbuf.toString()
  }

  private def withinBrackets(str: String) = s"[$str]"
}
