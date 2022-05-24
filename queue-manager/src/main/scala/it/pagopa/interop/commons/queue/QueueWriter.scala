package it.pagopa.interop.commons.queue

import scala.concurrent.Future
import it.pagopa.interop.commons.queue.impl.SQSWriter
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import scala.concurrent.ExecutionContext
import spray.json.JsValue

trait QueueWriter {
  def send(message: Message): Future[String]
  def sendBulk(messages: List[Message]): Future[List[String]]
}

object QueueWriter {

  def get(queueUrl: String)(f: PartialFunction[ProjectableEvent, JsValue])(implicit ec: ExecutionContext): QueueWriter =
    new SQSWriter(queueUrl)(f)
}
