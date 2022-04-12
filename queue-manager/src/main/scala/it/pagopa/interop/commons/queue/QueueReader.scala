package it.pagopa.interop.commons.queue

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import it.pagopa.interop.commons.queue.impl.SQSReader
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.Named
import spray.json.RootJsonFormat

trait QueueReader {
  def receiveN[T: Named](n: Int)(deser: PartialFunction[String, RootJsonFormat[T]]): Future[List[Message[T]]]
  def handleN[T: Named, V](n: Int)(deser: PartialFunction[String, RootJsonFormat[T]])(
    f: Message[T] => Future[V]
  ): Future[List[V]]
  def handle[T: Named, V](deser: PartialFunction[String, RootJsonFormat[T]])(f: Message[T] => Future[V]): Future[Unit]
}

object QueueReader {

  def get[T: Named](implicit ec: ExecutionContext): Try[QueueReader] =
    QueueConfiguration.queueImplementation match {
      case "aws" => Success(new SQSReader(QueueConfiguration.queueAccountInfo))
      case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
    }
}
