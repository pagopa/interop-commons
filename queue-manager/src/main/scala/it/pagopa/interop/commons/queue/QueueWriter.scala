package it.pagopa.interop.commons.queue

import scala.concurrent.Future
import it.pagopa.interop.commons.queue.QueueConfiguration
import it.pagopa.interop.commons.queue.impl.SQSWriter
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.Named
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}
import spray.json.RootJsonFormat

trait QueueWriter[T] {

  def send(message: Message[T]): Future[String]
  def sendBulk(messages: List[Message[T]]): Future[List[String]]
}

object QueueWriter {

  def get[T: Named](implicit ec: ExecutionContext, x: RootJsonFormat[T]): Try[QueueWriter[T]] =
    QueueConfiguration.queueImplementation match {
      case "aws" => Success(new SQSWriter[T](QueueConfiguration.queueAccountInfo))
      case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
    }
}
