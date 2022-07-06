package it.pagopa.interop.commons.queue.impl

import cats.implicits.toFunctorOps
import software.amazon.awssdk.services.sqs.model.{
  DeleteMessageRequest,
  Message,
  ReceiveMessageRequest,
  SendMessageRequest
}
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import scala.jdk.FutureConverters._
import com.typesafe.config.ConfigFactory

final case class SQSHandler(queueUrl: String)(blockingExecutionContext: ExecutionContextExecutor) {

  private implicit val ec: ExecutionContextExecutor = blockingExecutionContext

  private lazy val concurrency: Int = ConfigFactory.load().getInt("interop-commons.queue-manager.max-concurrency")

  private val asyncHttpClient: SdkAsyncHttpClient =
    NettyNioAsyncHttpClient.builder().maxConcurrency(concurrency).build()

  private val asyncConfiguration: ClientAsyncConfiguration = ClientAsyncConfiguration
    .builder()
    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, blockingExecutionContext)
    .build()

  private val sqsClient: SqsAsyncClient =
    SqsAsyncClient.builder().httpClient(asyncHttpClient).asyncConfiguration(asyncConfiguration).build()

  def send[T: JsonWriter](message: T): Future[String] = {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageBody(message.toJson.compactPrint)
      .build()
    sqsClient.sendMessage(sendMsgRequest).asScala.map(_.messageId())
  }

  def deleteMessage(receiptHandle: String): Future[Unit] = {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(receiptHandle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest).asScala.void
  }

  /** Fetches 10 messages at time until it reaches `chunkSize` and gives you back 
    * both the raw messages and the handles. Runs until the queue is empty*/
  def processAllRawMessages(chunkSize: Int, visibilityTimeout: Int)(
    fn: (List[String], List[String]) => Future[Unit]
  ): Future[Unit] = loop(rawReceiveNBodyAndHandle(10, visibilityTimeout))(chunkSize, Nil)(fn)

  /** Fetches 10 messages at time until it reaches `chunkSize` and gives you back 
    * both the deserialized messages and the handles. Runs until the queue is empty*/
  def processAllMessages[T: JsonReader](chunkSize: Int, visibilityTimeout: Int)(
    fn: (List[T], List[String]) => Future[Unit]
  ): Future[Unit] = loop(processMessages[T, T](10, visibilityTimeout)(Future.successful))(chunkSize, Nil)(fn)

  private def loop[T](getMessages: => Future[List[(T, String)]])(chunkSize: Int, acc: List[(T, String)])(
    fn: (List[T], List[String]) => Future[Unit]
  ) = innerLoop(accumulateMessages(getMessages)(chunkSize, acc))(fn)

  private def innerLoop[T](
    accumulatedMessages: => Future[List[(T, String)]]
  )(fn: (List[T], List[String]) => Future[Unit]): Future[Unit] =
    accumulatedMessages.flatMap { messagesAndHandles =>
      if (messagesAndHandles.isEmpty) Future.unit
      else fn.tupled(messagesAndHandles.unzip[T, String]).flatMap(_ => innerLoop[T](accumulatedMessages)(fn))
    }

  private def accumulateMessages[T](
    getMessages: => Future[List[(T, String)]]
  )(chunkSize: Int, acc: List[(T, String)]): Future[List[(T, String)]] =
    getMessages.flatMap { messages =>
      val chunk: List[(T, String)] = acc ++ messages
      if (messages.isEmpty) Future.successful(acc)
      else if (chunk.size >= chunkSize) Future.successful(chunk)
      else accumulateMessages(getMessages)(chunkSize, chunk)
    }

  private def processMessages[T: JsonReader, V](maxNumberOfMessages: Int, visibilityTimeout: Int)(
    fn: T => Future[V]
  ): Future[List[(V, String)]] = for {
    messagesAndHandles <- rawReceiveNBodyAndHandle(maxNumberOfMessages, visibilityTimeout)
    result             <- Future.traverse(messagesAndHandles) { case (m, h) => deserialize(m).flatMap(fn).map((_, h)) }
  } yield result

  private def deserialize[T: JsonReader](s: String): Future[T] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[T]))
  }

  private def rawReceiveNBodyAndHandle(n: Int, visibilityTimeout: Int): Future[List[(String, String)]] =
    rawReceiveN(n, visibilityTimeout).map(_.map(m => (m.body(), m.receiptHandle())))

  private def rawReceiveN(n: Int, visibilityTimeout: Int): Future[List[Message]] = {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .visibilityTimeout(visibilityTimeout)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).asScala.map(_.messages().asScala.toList)
  }

}
