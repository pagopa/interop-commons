package it.pagopa.interop.commons.queue.impl

import cats.implicits.{toFunctorOps, toTraverseOps}
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{
  DeleteMessageRequest,
  Message,
  ReceiveMessageRequest,
  SendMessageRequest
}
import spray.json.{JsonWriter, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

final case class SQSHandler(queueUrl: String)(implicit ec: ExecutionContext) {

  private val sqsClient: SqsClient = SqsClient.create()

  def send[T: JsonWriter](message: T): Future[String] = Future {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageBody(message.toJson.compactPrint)
      .build()
    sqsClient.sendMessage(sendMsgRequest).messageId()
  }

  def deleteMessage(receiptHandle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(receiptHandle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  def processMessages[T: JsonReader, V](maxNumberOfMessages: Int, visibilityTimeout: Int)(
    fn: T => Future[V]
  ): Future[List[(V, String)]] = for {
    messagesAndHandles <- rawReceiveNBodyAndHandle(maxNumberOfMessages, visibilityTimeout)
    result             <- Future.traverse(messagesAndHandles) { case (m, h) => toJson(m).flatMap(fn).map((_, h)) }
  } yield result

  /** Fetches 10 messages at time until it reaches `chunkSize` and gives you back 
    * both the deserialized messages and the handles. Runs until the queue is empty*/
  def processAllMessages[T: JsonReader](chunkSize: Int, visibilityTimeout: Int)(
    fn: (List[T], List[String]) => Future[Unit]
  ): Future[Unit] = {

    def accumulateMessages(acc: List[(T, String)]): Future[List[(T, String)]] =
      processMessages[T, T](10, visibilityTimeout)(Future.successful).flatMap { messages =>
        val chunk: List[(T, String)] = acc ++ messages
        if (messages.isEmpty) Future.successful(acc)
        else if (chunk.size >= chunkSize) Future.successful(chunk)
        else accumulateMessages(chunk)
      }

    def loop: Future[Unit] = accumulateMessages(Nil).flatMap { messagesAndHandles =>
      if (messagesAndHandles.isEmpty) Future.unit
      else fn.tupled(messagesAndHandles.unzip[T, String]).flatMap(_ => loop)
    }

    loop
  }

  private def toJson[T: JsonReader](s: String): Future[T] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[T]))
  }

  private def rawReceiveNBodyAndHandle(n: Int, visibilityTimeout: Int): Future[List[(String, String)]] =
    rawReceiveN(n, visibilityTimeout).map(_.map(m => (m.body(), m.receiptHandle())))

  private def rawReceiveN(n: Int, visibilityTimeout: Int): Future[List[Message]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .visibilityTimeout(visibilityTimeout)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

}
