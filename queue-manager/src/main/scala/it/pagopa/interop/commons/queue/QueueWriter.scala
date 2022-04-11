package it.pagopa.interop.commons.queue

import scala.concurrent.Future
import it.pagopa.interop.commons.queue.QueueConfiguration
import it.pagopa.interop.commons.queue.impl.SQSWriter
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.JsonSerde
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

trait QueueWriter {

  def send[T: JsonSerde](message: Message[T]): Future[String]
  def sendBulk[T: JsonSerde](messages: List[Message[T]]): Future[List[String]]
}

object QueueWriter {

  def get(implicit ec: ExecutionContext): Try[QueueWriter] = QueueConfiguration.queueImplementation match {
    case "aws" => Success(new SQSWriter(QueueConfiguration.queueAccountInfo))
    case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
  }
}
