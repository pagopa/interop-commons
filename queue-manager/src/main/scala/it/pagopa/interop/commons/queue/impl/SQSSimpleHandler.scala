package it.pagopa.interop.commons.queue.impl

import cats.implicits.{toFunctorOps, toTraverseOps}
import it.pagopa.interop.commons.queue.QueueAccountInfo
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
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

final case class SQSSimpleHandler(queueAccountInfo: QueueAccountInfo, queueUrl: String)(implicit ec: ExecutionContext) {

  private val awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(queueAccountInfo.accessKeyId, queueAccountInfo.secretAccessKey)

  private val sqsClient: SqsClient = SqsClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .region(queueAccountInfo.region)
    .build()

  def send[T: JsonWriter](message: T): Future[String] = Future {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageBody(message.toJson.compactPrint)
      .build()

    sqsClient.sendMessage(sendMsgRequest).messageId()

  }

  /**
    * Processes a maximum number of messages with a defined visibility timeout and process each of them 
    * with the <code>fn</code> function.
    *
    * @param maxNumberOfMessages
    * maximum number of messages processed
    * @param visibilityTimeout
    * The duration (in seconds) that the received messages are hidden from subsequent retrieve requests
    * after being retrieved by a <code>ReceiveMessage</code> request.
    * @param fn
    * function processing each message payload
    * @tparam T
    * type representing the domain model payload
    * @return
    * sequence of processed messages
    */
  def processMessages[T, V](maxNumberOfMessages: Int, visibilityTimeout: Int)(
    fn: T => Future[V]
  )(implicit jsonReader: JsonReader[T]): Future[List[V]] = for {
    messagesAndReceiptHandles <- deserializeMessages(maxNumberOfMessages, visibilityTimeout)
    result                    <- messagesAndReceiptHandles.traverse { case (message, handle) =>
      handleMessageAndDelete(fn)(message, handle)
    }
  } yield result

  private def deserializeMessages[T](n: Int, visibilityTimeout: Int)(implicit
    jsonReader: JsonReader[T]
  ): Future[List[(T, String)]] = for {
    messages <- rawReceiveN(n, visibilityTimeout)
    bodyAndHandle = messages.map(m => (m.body(), m.receiptHandle()))
    messagesAndHandles <- bodyAndHandle.traverse { case (body, handle) => toMessage(body).map((_, handle)) }
  } yield messagesAndHandles

  private def toMessage[T](s: String)(implicit jsonReader: JsonReader[T]): Future[T] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[T]))
  }

  private def rawReceiveN(n: Int, visibilityTimeout: Int): Future[List[Message]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .visibilityTimeout(visibilityTimeout)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.void

  private def handleMessageAndDelete[T, V](
    processMessageBody: T => Future[V]
  )(message: T, receiptHandle: String): Future[V] =
    processMessageBody(message).flatMap(v => deleteMessage(receiptHandle).as(v))
}
