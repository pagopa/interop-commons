package it.pagopa.interop.commons.queue.impl

import it.pagopa.interop.commons.queue.QueueAccountInfo
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import spray.json.{JsonWriter, _}

import scala.concurrent.{ExecutionContext, Future}

final case class SQSSimpleWriter(queueAccountInfo: QueueAccountInfo, queueUrl: String)(implicit ec: ExecutionContext) {

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

}
