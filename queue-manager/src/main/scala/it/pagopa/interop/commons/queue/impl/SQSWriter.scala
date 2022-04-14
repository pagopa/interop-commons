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
import it.pagopa.interop.commons.queue.message.ProjectableEvent
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

final class SQSWriter(queueAccountInfo: QueueAccountInfo, queueUrl: String)(
  f: PartialFunction[ProjectableEvent, JsValue]
)(implicit ec: ExecutionContext)
    extends QueueWriter {

  private val CONSTANT_MESSAGE_GROUP_ID: String = "message_group_all_notification"
  implicit private val w: JsonWriter[Message]   = Message.messageWriter(f)

  private val awsCredentials: AwsBasicCredentials =
    AwsBasicCredentials.create(queueAccountInfo.accessKeyId, queueAccountInfo.secretAccessKey)

  private val sqsClient: SqsClient = SqsClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .region(Region.EU_CENTRAL_1)
    .build()

  override def send(message: Message): Future[String] = Future {
    val sendMsgRequest: SendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageBody(message.toJson.compactPrint)
      .messageGroupId(CONSTANT_MESSAGE_GROUP_ID)
      .messageDeduplicationId(s"${message.eventJournalPersistenceId}_${message.eventJournalSequenceNumber}")
      .build()

    sqsClient.sendMessage(sendMsgRequest).messageId()
  }

  override def sendBulk(messages: List[Message]): Future[List[String]] = Future {
    def messageAdapter(m: Message): SendMessageBatchRequestEntry = SendMessageBatchRequestEntry
      .builder()
      // it is used to track the eventual failure for this specific message
      .id(UUID.randomUUID().toString())
      .messageBody(m.toJson.compactPrint)
      .messageGroupId(CONSTANT_MESSAGE_GROUP_ID)
      .messageDeduplicationId(s"${m.eventJournalPersistenceId}_${m.eventJournalSequenceNumber}")
      .build()

    val sendMessageBatchRequest = SendMessageBatchRequest
      .builder()
      .queueUrl(queueUrl)
      .entries(messages.map(messageAdapter).asJavaCollection)
      .build()

    val response = sqsClient.sendMessageBatch(sendMessageBatchRequest)

    response.successful().asScala.map(_.id()).toList.appendedAll(response.failed().asScala.map(_.id()).toList)
  }

}
