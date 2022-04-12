package it.pagopa.interop.commons.queue

import scala.concurrent.Future
import it.pagopa.interop.commons.queue.QueueConfiguration
import it.pagopa.interop.commons.queue.impl.SQSWriter
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}
import spray.json.RootJsonFormat
import spray.json.JsValue

trait QueueWriter {
  def send(message: Message): Future[String]
  def sendBulk(messages: List[Message]): Future[List[String]]
}

object QueueWriter {

  def get(f: PartialFunction[ProjectableEvent, JsValue])(implicit ec: ExecutionContext): Try[QueueWriter] =
    QueueConfiguration.queueImplementation match {
      case "aws" => Success(new SQSWriter(QueueConfiguration.queueAccountInfo)(f))
      case x     => Failure(new RuntimeException(s"Unsupported queue implementation: $x"))
    }
}
