package it.pagopa.interop.commons.queue

import scala.concurrent.{Future, ExecutionContext}
import it.pagopa.interop.commons.queue.impl.SQSReader
import it.pagopa.interop.commons.queue.message.Message
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import spray.json.JsValue

trait QueueReader {
  def receiveN(n: Int): Future[List[Message]]
  def handleN[V](n: Int)(f: Message => Future[V]): Future[List[V]]
  def handle[V](f: Message => Future[V]): Future[Unit]
}

object QueueReader {

  def get(queueUrl: String, visibilityTimeoutInSeconds: Integer = 30)(
    f: PartialFunction[String, JsValue => ProjectableEvent]
  )(implicit ec: ExecutionContext): QueueReader =
    new SQSReader(queueUrl, visibilityTimeoutInSeconds)(f)
}
