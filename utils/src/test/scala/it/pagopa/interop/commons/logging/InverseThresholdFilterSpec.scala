package it.pagopa.interop.commons.logging

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{FileAppender, LayoutBase}
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.filter.Filter
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

class InverseThresholdFilterSpec extends AnyWordSpecLike with Matchers {

  def getStartedEncoder(implicit loggingContext: LoggerContext): LayoutWrappingEncoder[ILoggingEvent] = {
    val simpleLoggingLayout: LayoutBase[ILoggingEvent] = (event: ILoggingEvent) => {
      val level: String      = event.getLevel.toString()
      val loggerName: String = event.getLoggerName
      val message: String    = event.getFormattedMessage()
      s"[$level] $loggerName $message\n"
    }
    simpleLoggingLayout.setContext(loggingContext)
    simpleLoggingLayout.start()

    val encoder: LayoutWrappingEncoder[ILoggingEvent] = new LayoutWrappingEncoder[ILoggingEvent]
    encoder.setContext(loggingContext)
    encoder.setLayout(simpleLoggingLayout)
    encoder.start()
    encoder
  }

  def getLoggingFilter(level: Level)(implicit loggingContext: LoggerContext): ThresholdFilter = {
    val filter = new ThresholdFilter
    filter.setContext(loggingContext)
    filter.setLevel(level.toString())
    filter.start()
    filter
  }

  def getInverseLoggingFilter(level: Level)(implicit loggingContext: LoggerContext): InverseThresholdFilter = {
    val filter = new InverseThresholdFilter
    filter.setContext(loggingContext)
    filter.setLevel(level.toString())
    filter.start()
    filter
  }

  def getFileAppender(filePath: Path, filter: Filter[ILoggingEvent])(implicit
    loggingContext: LoggerContext
  ): FileAppender[ILoggingEvent] = {
    val appender = new FileAppender[ILoggingEvent]
    appender.setContext(loggingContext)
    appender.setFile(filePath.toAbsolutePath.toString)
    appender.setEncoder(getStartedEncoder)
    appender.addFilter(filter)
    appender.start()
    appender
  }

  def createConfiguredLogger(threshold: Level): (Logger, Path, Path) = {
    implicit val loggingContext: LoggerContext     = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggingContext.reset()
    val logger: Logger                             = loggingContext.getLogger("InverseThresholdFilterSpec")
    val errorLog: Path                             = Files.createTempFile("stderr", "log")
    val standardLog: Path                          = Files.createTempFile("stdout", "log")
    val errorAppender: FileAppender[ILoggingEvent] = getFileAppender(errorLog, getLoggingFilter(threshold))
    val stdoutAppender                             = getFileAppender(standardLog, getInverseLoggingFilter(threshold))
    logger.addAppender(errorAppender)
    logger.addAppender(stdoutAppender)
    (logger, errorLog, standardLog)
  }

  "A propely configured Logger" should {
    "log errors in the error log and the rest in the standard log" in {
      val (logger, errorLog, standardLog) = createConfiguredLogger(Level.ERROR)
      logger.setLevel(Level.TRACE)

      logger.trace("This is a trace message")
      logger.debug("This is a debug message")
      logger.info("This is a info message")
      logger.warn("This is a warn message")
      logger.error("This is an error message")

      Files.readAllLines(errorLog).asScala.toList shouldBe List(
        "[ERROR] InverseThresholdFilterSpec This is an error message"
      )

      Files.readAllLines(standardLog).asScala.toList shouldBe List(
        "[TRACE] InverseThresholdFilterSpec This is a trace message",
        "[DEBUG] InverseThresholdFilterSpec This is a debug message",
        "[INFO] InverseThresholdFilterSpec This is a info message",
        "[WARN] InverseThresholdFilterSpec This is a warn message"
      )
    }

    "log errors and warns in the error log and the rest in the standard log" in {
      val (logger, errorLog, standardLog) = createConfiguredLogger(Level.WARN)
      logger.setLevel(Level.TRACE)

      logger.trace("This is a trace message")
      logger.debug("This is a debug message")
      logger.info("This is a info message")
      logger.warn("This is a warn message")
      logger.error("This is an error message")

      Files.readAllLines(errorLog).asScala.toList shouldBe List(
        "[WARN] InverseThresholdFilterSpec This is a warn message",
        "[ERROR] InverseThresholdFilterSpec This is an error message"
      )

      Files.readAllLines(standardLog).asScala.toList shouldBe List(
        "[TRACE] InverseThresholdFilterSpec This is a trace message",
        "[DEBUG] InverseThresholdFilterSpec This is a debug message",
        "[INFO] InverseThresholdFilterSpec This is a info message"
      )
    }

    "log errors and warns in the error log and the rest in the standard log according to its level" in {
      val (logger, errorLog, standardLog) = createConfiguredLogger(Level.WARN)
      logger.setLevel(Level.INFO)

      logger.trace("This is a trace message")
      logger.debug("This is a debug message")
      logger.info("This is a info message")
      logger.warn("This is a warn message")
      logger.error("This is an error message")

      Files.readAllLines(errorLog).asScala.toList shouldBe List(
        "[WARN] InverseThresholdFilterSpec This is a warn message",
        "[ERROR] InverseThresholdFilterSpec This is an error message"
      )

      Files.readAllLines(standardLog).asScala.toList shouldBe List(
        "[INFO] InverseThresholdFilterSpec This is a info message"
      )
    }
  }

}
