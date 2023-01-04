package it.pagopa.interop.commons.logging

import ch.qos.logback.classic.spi.{ILoggingEvent, IThrowableProxy, ThrowableProxyUtil}
import ch.qos.logback.core.CoreConstants.LINE_SEPARATOR
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.util.CachingDateFormatter
import scala.jdk.CollectionConverters.MapHasAsScala
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, IP_ADDRESS, ORGANIZATION_ID_CLAIM, SUB, UID}

final class LoggerLayout extends LayoutBase[ILoggingEvent] {

  val cachingDateFormatter = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss.SSS")

  override def doLayout(event: ILoggingEvent): String = {
    val sbuf: StringBuffer                      = new StringBuffer(128)
    val time: String                            = cachingDateFormatter.format(event.getTimeStamp)
    val level: String                           = event.getLevel.toString()
    val loggerName: String                      = event.getLoggerName
    val message: String                         = event.getFormattedMessage()
    val throwableProxy: Option[IThrowableProxy] = Option(event.getThrowableProxy())

    val mdc: Map[String, String] = event.getMdc().asScala.toMap
    val cid: String              = mdc.get(CORRELATION_ID_HEADER).fold("[CID=]")(s => s"[CID=$s]")
    val header: String           = List(
      mdc.get(IP_ADDRESS).fold("[IP=]")(s => s"[IP=$s]"),
      mdc.get(UID).orElse(mdc.get(SUB)).fold("[UID=]")(s => s"[UID=$s]"),
      mdc.get(ORGANIZATION_ID_CLAIM).fold("[OID=]")(s => s"[OID=$s]"),
      cid
    ).mkString(" ")

    sbuf.append(s"$time $level [$loggerName] - $header $message")
    sbuf.append(LINE_SEPARATOR)
    throwableProxy
      .map(ThrowableProxyUtil.asString)
      .map(_.split("\n").map(l => s"${cid} $l").mkString("", "\n", "\n"))
      .foreach(sbuf.append)
    sbuf.toString()
  }

}
