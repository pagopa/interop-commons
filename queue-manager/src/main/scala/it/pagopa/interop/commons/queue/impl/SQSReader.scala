package it.pagopa.interop.commons.queue.impl

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import scala.concurrent.Future
import it.pagopa.interop.commons.queue.message.Named
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

final class SQSReader(queueAccountInfo: QueueAccountInfo)(implicit ec: ExecutionContext) extends QueueReader {

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

  private def from[T: Named](deser: PartialFunction[String, RootJsonFormat[T]])(s: String): Try[Message[T]] = {
    implicit val mf = Message.jsonReader(deser)
    Try(s.parseJson.convertTo[Message[T]])
  }

  override def receiveN[T: Named](
    n: Int
  )(deser: PartialFunction[String, RootJsonFormat[T]]): Future[List[Message[T]]] = {
    implicit val reader = Message.jsonReader(deser)
    for {
      rawMessages <- rawReceiveN(n)
      messages    <- Future.fromTry(rawMessages.map(_.body()).traverse(from(deser)))
    } yield messages
  }

  override def handleN[T: Named, V](
    n: Int
  )(deser: PartialFunction[String, RootJsonFormat[T]])(f: Message[T] => Future[V]): Future[List[V]] = for {
    rawMessages        <- rawReceiveN(n)
    messagesAndHandles <- rawMessages.traverse(message =>
      Future.fromTry(from(deser)(message.body())).map((_, message.receiptHandle()))
    )
    result             <- messagesAndHandles
      .traverseFilter { case (message, handle) =>
        (f(message) <* deleteMessage(handle)).map(_.some).recover { case _ => None }
      }
  } yield result

  override def handle[T: Named, V](
    deser: PartialFunction[String, RootJsonFormat[T]]
  )(f: Message[T] => Future[V]): Future[Unit] = {
    // Submitting to an ExecutionContext introduces an async boundary that reset the stack,
    // that makes Future behave like it's trampolining, so this function is stack safe.
    def loop: Future[List[V]] = handleN[T, V](10)(deser)(f).flatMap(_ => loop).recoverWith(_ => loop)
    loop.void
  }

}
