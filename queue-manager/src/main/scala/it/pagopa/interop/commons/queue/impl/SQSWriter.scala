package it.pagopa.interop.commons.queue.impl

import scala.concurrent.ExecutionContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import it.pagopa.interop.commons.queue.message.Message
import scala.concurrent.Future
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import java.util.UUID
import it.pagopa.interop.commons.queue.QueueAccountInfo
import it.pagopa.interop.commons.queue.QueueWriter
import it.pagopa.interop.commons.queue.message.Named
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import spray.json.RootJsonFormat
import spray.json.JsonWriter
import spray.json._

final class SQSWriter[T: Named](queueAccountInfo: QueueAccountInfo)(implicit ec: ExecutionContext, x: RootJsonFormat[T])
    extends QueueWriter[T] {

  implicit private val w: JsonWriter[Message[T]] = Message.jsonWriter

  private val awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(queueAccountInfo.accessKeyId, queueAccountInfo.secretAccessKey)

  private val sqsClient: SqsClient = SqsClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .region(Region.EU_CENTRAL_1)
    .build()

  override def send(message: Message[T]): Future[String] = Future {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .messageBody(message.toJson(w).compactPrint)
      .messageGroupId(s"${message.eventJournalPersistenceId}_${message.eventJournalSequenceNumber}")
      .messageDeduplicationId(s"${message.eventJournalPersistenceId}_${message.eventJournalSequenceNumber}")
      .build()

    sqsClient.sendMessage(sendMsgRequest).messageId()
  }

  override def sendBulk(messages: List[Message[T]]): Future[List[String]] = Future {
    assert(messages.size <= 10, "Amazon SQS supports a bulk of maximum 10 messages")

    def messageAdapter(m: Message[T]): SendMessageBatchRequestEntry = SendMessageBatchRequestEntry
      .builder()
      // it is used to track the eventual failure for this specific message
      .id(UUID.randomUUID().toString())
      .messageBody(m.toJson.compactPrint)
      .messageGroupId(s"${m.eventJournalPersistenceId}_${m.eventJournalSequenceNumber}")
      .messageDeduplicationId(s"${m.eventJournalPersistenceId}_${m.eventJournalSequenceNumber}")
      .build()

    val sendMessageBatchRequest = SendMessageBatchRequest
      .builder()
      .queueUrl(queueAccountInfo.queueUrl)
      .entries(messages.map(messageAdapter).asJavaCollection)
      .build()

    val response = sqsClient.sendMessageBatch(sendMessageBatchRequest)

    response.successful().asScala.map(_.id()).toList.appendedAll(response.failed().asScala.map(_.id()).toList)
  }

}
