package it.pagopa.interop.commons.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

final class InverseThresholdFilter extends Filter[ILoggingEvent] {

  var level: Level = null

  override def decide(event: ILoggingEvent): FilterReply =
    if (isStarted() && event.getLevel().isGreaterOrEqual(level)) FilterReply.DENY else FilterReply.NEUTRAL

  def setLevel(level: String): Unit = this.level = Level.toLevel(level)

  override def start(): Unit = if (this.level != null) { super.start() }
}
