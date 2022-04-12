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
import it.pagopa.interop.commons.queue.{QueueAccountInfo}
import it.pagopa.interop.commons.queue.message.Message
import cats.syntax.all._
import it.pagopa.interop.commons.queue.QueueReader
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import spray.json.RootJsonFormat
import scala.util.Try
import spray.json._

final class SQSReader(queueAccountInfo: QueueAccountInfo)(f: PartialFunction[String, JsValue => ProjectableEvent])(
  implicit ec: ExecutionContext
) extends QueueReader {

  implicit val messageReader: JsonReader[Message] = Message.messageReader(f)

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
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  private def from(s: String): Try[Message] = Try(s.parseJson.convertTo[Message])

  override def receiveN(n: Int): Future[List[Message]] = {
    for {
      rawMessages <- rawReceiveN(n)
      messages    <- Future.fromTry(rawMessages.map(_.body()).traverse(from(_)))
    } yield messages
  }

  override def handleN[V](n: Int)(f: Message => Future[V]): Future[List[V]] = for {
    rawMessages        <- rawReceiveN(n)
    messagesAndHandles <- rawMessages.traverse(message =>
      Future.fromTry(from(message.body())).map((_, message.receiptHandle()))
    )
    result             <- messagesAndHandles
      .traverseFilter { case (message, handle) =>
        (f(message) <* deleteMessage(handle)).map(_.some).recover { case _ => None }
      }
  } yield result

  override def handle[V](f: Message => Future[V]): Future[Unit] = {
    // Submitting to an ExecutionContext introduces an async boundary that reset the stack,
    // that makes Future behave like it's trampolining, so this function is stack safe.
    def loop: Future[List[V]] = handleN[V](10)(f).flatMap(_ => loop).recoverWith(_ => loop)
    loop.void
  }

}
