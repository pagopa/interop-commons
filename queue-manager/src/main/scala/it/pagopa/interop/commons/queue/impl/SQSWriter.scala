package it.pagopa.interop.commons.queue.impl

import it.pagopa.interop.commons.queue.QueueWriter
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{
  SendMessageBatchRequest,
  SendMessageBatchRequestEntry,
  SendMessageRequest
}
import spray.json.{JsonWriter, _}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

final class SQSWriter(queueUrl: String)(f: PartialFunction[ProjectableEvent, JsValue])(implicit ec: ExecutionContext)
    extends QueueWriter {

  private val CONSTANT_MESSAGE_GROUP_ID: String = "message_group_all_notification"
  implicit private val w: JsonWriter[Message]   = Message.messageWriter(f)

  private val sqsClient: SqsClient = SqsClient.create()

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
      .id(UUID.randomUUID().toString)
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
