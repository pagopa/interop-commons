package it.pagopa.interop.commons.queue.impl

import cats.syntax.all._
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, Message => SQSMessage}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

final case class SQSSimpleReader(queueUrl: String)(implicit ec: ExecutionContext) {

  final case class MessageWithHandle[T: JsonReader](message: T, handle: String)

  private val sqsClient: SqsClient = SqsClient.builder().build()

  def receiveN[T: JsonReader](n: Int): Future[List[MessageWithHandle[T]]] = receiveMessageAndHandleN(n)

  def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  private def receiveMessageAndHandleN[T: JsonReader](n: Int): Future[List[MessageWithHandle[T]]] = for {
    messages <- rawReceiveN(n)
    bodyAndHandle = messages.map(m => (m.body(), m.receiptHandle()))
    messagesAndHandles <- bodyAndHandle.traverse { case (body, handle) => deserialize(body).map((_, handle)) }
  } yield messagesAndHandles.map(mh => MessageWithHandle(mh._1, mh._2))

  private def rawReceiveN(n: Int): Future[List[SQSMessage]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def deserialize[T: JsonReader](s: String): Future[T] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[T]))
  }

}
