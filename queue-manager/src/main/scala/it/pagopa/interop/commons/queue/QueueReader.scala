package it.pagopa.interop.commons.queue

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import it.pagopa.interop.commons.queue.impl.SQSReader
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.JsonSerde

trait QueueReader {
  def receiveN[T: JsonSerde](n: Int): Future[List[Message[T]]]
  def handleN[T: JsonSerde, V](n: Int)(f: Message[T] => Future[V]): Future[List[V]]
  def handle[T: JsonSerde, V](f: Message[T] => Future[V]): Future[Unit]
}

object QueueReader {

  def get(implicit ec: ExecutionContext): Try[QueueReader] = QueueConfiguration.queueImplementation match {
    case "aws" => Success(new SQSReader(QueueConfiguration.queueAccountInfo))
    case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
  }
}
