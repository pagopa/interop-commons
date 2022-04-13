package it.pagopa.interop.commons.queue.impl

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import scala.concurrent.Future
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import it.pagopa.interop.commons.queue.QueueAccountInfo
import it.pagopa.interop.commons.queue.message.Message
import cats.syntax.all._
import it.pagopa.interop.commons.queue.QueueReader
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import spray.json.RootJsonFormat
import scala.util.Try
import spray.json._
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest
import it.pagopa.interop.commons.queue.QueueConfiguration
import scala.util.Failure
import scala.util.Success

final class SQSReader(queueAccountInfo: QueueAccountInfo)(f: PartialFunction[String, JsValue => ProjectableEvent])(
  implicit ec: ExecutionContext
) extends QueueReader {

  private val VISIBILITY_TIMEOUT_IN_SECONDS: Int          = QueueConfiguration.visibilityTimeout
  implicit private val messageReader: JsonReader[Message] = Message.messageReader(f)

  private val awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(queueAccountInfo.accessKeyId, queueAccountInfo.secretAccessKey)

  private val sqsClient: SqsClient = SqsClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .region(Region.EU_CENTRAL_1)
    .build()

  private def rawReceiveN(n: Int): Future[List[SQSMessage]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .maxNumberOfMessages(n)
      .visibilityTimeout(VISIBILITY_TIMEOUT_IN_SECONDS)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def rawReceiveBodyAndHandleN(n: Int): Future[List[(String, String)]] = for {
    messages <- rawReceiveN(n)
  } yield messages.map(m => (m.body(), m.receiptHandle()))

  private def toMessage(s: String): Future[Message] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(new DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[Message]))
  }

  private def receiveMessageAndHandleN(n: Int): Future[List[(Message, String)]] = for {
    messages <- rawReceiveN(n)
    bodyAndHandle = messages.map(m => (m.body(), m.receiptHandle()))
    messagesAndHandles <- bodyAndHandle.traverse { case (body, handle) => toMessage(body).map((_, handle)) }
  } yield messagesAndHandles

  override def receiveN(n: Int): Future[List[Message]] = for {
    messagesAndHandles <- receiveMessageAndHandleN(n)
  } yield messagesAndHandles.map(_._1)

  private def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  private def waitTimeout: Future[Unit] = Future(Thread.sleep(VISIBILITY_TIMEOUT_IN_SECONDS * 1000))

  private def reEnqueueOrWait(receiptHandle: String): Future[Unit] = Future {
    val request: ChangeMessageVisibilityRequest = ChangeMessageVisibilityRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .receiptHandle(receiptHandle)
      .visibilityTimeout(0)
      .build()
    sqsClient.changeMessageVisibility(request)
  }.void.recoverWith(_ => waitTimeout)

  private def handleAndDeleteSingleMessageOrReEnqueue[V](
    f: Message => Future[V]
  )(m: Message, handle: String): Future[V] =
    f(m)
      .flatMap(v => deleteMessage(handle).as(v))
      .recoverWith { case e => reEnqueueOrWait(handle) >> Future.failed(e) }

  override def handleN[V](n: Int)(f: Message => Future[V]): Future[List[V]] = for {
    messagesAndHandles <- receiveMessageAndHandleN(n)
    result             <- messagesAndHandles.traverse { case (message, handle) =>
      handleAndDeleteSingleMessageOrReEnqueue(f)(message, handle)
    }
  } yield result

  override def handle[V](f: Message => Future[V]): Future[Unit] = {
    // Submitting to an ExecutionContext introduces an async boundary that reset the stack,
    // that makes Future behave like it's trampolining, so this function is stack safe.
    def loop: Future[List[V]] = handleN[V](10)(f).flatMap(_ => loop).recoverWith(_ => loop)
    loop.void
  }

}
