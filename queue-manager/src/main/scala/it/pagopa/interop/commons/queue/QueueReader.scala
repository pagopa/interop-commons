package it.pagopa.interop.commons.queue

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import it.pagopa.interop.commons.queue.impl.SQSReader
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.Event
import spray.json.RootJsonFormat
import spray.json.JsValue

trait QueueReader {
  def receiveN(n: Int): Future[List[Message]]
  def handleN[V](n: Int)(f: Message => Future[V]): Future[List[V]]
  def handle[V](f: Message => Future[V]): Future[Unit]
}

object QueueReader {

  def get(f: PartialFunction[String, JsValue => Event])(implicit ec: ExecutionContext): Try[QueueReader] =
    QueueConfiguration.queueImplementation match {
      case "aws" => Success(new SQSReader(QueueConfiguration.queueAccountInfo)(f))
      case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
    }
}
