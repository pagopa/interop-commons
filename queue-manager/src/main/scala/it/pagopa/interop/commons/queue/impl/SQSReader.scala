package it.pagopa.interop.commons.queue.impl

import it.pagopa.interop.commons.queue.QueueReader
import it.pagopa.interop.commons.queue.message.{Message, ProjectableEvent}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, Message => SQSMessage}
import spray.json._
import it.pagopa.interop.commons.utils.TypeConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

final class SQSReader(queueUrl: String, visibilityTimeout: Integer)(
  f: PartialFunction[String, JsValue => ProjectableEvent]
)(implicit ec: ExecutionContext)
    extends QueueReader {

  implicit private val messageReader: JsonReader[Message] = Message.messageReader(f)

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val sqsClient: SqsClient = SqsClient.create()

  private def rawReceiveN(n: Int): Future[List[SQSMessage]] = Future {
    val receiveMessageRequest: ReceiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(n)
      .visibilityTimeout(visibilityTimeout)
      .build()
    sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
  }

  private def toMessage(s: String): Future[Message] = Try(s.parseJson) match {
    case Failure(_)    => Future.failed(DeserializationException(s"Not a valid json: $s"))
    case Success(json) => Future.fromTry(Try(json.convertTo[Message]))
  }

  private def receiveMessageAndHandleN(n: Int): Future[List[(Message, String)]] = for {
    messages <- rawReceiveN(n)
    bodyAndHandle = messages.map(m => (m.body(), m.receiptHandle()))
    messagesAndHandles <- Future.sequentially(bodyAndHandle) { case (body, handle) => toMessage(body).map((_, handle)) }
  } yield messagesAndHandles

  override def receiveN(n: Int): Future[List[Message]] = for {
    messagesAndHandles <- receiveMessageAndHandleN(n)
  } yield messagesAndHandles.map(_._1)

  private def deleteMessage(handle: String): Future[Unit] = Future {
    val deleteMessageRequest: DeleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(handle)
      .build()
    sqsClient.deleteMessage(deleteMessageRequest)
  }.map(_ => ())

  private def handleMessageAndDelete[V](f: Message => Future[V])(m: Message, handle: String): Future[V] =
    f(m).flatMap(v => deleteMessage(handle).map(_ => v))

  override def handleN[V](n: Int)(f: Message => Future[V]): Future[List[V]] = for {
    messagesAndHandles <- receiveMessageAndHandleN(n)
    result             <- Future.sequentially(messagesAndHandles) { case (message, handle) =>
      handleMessageAndDelete(f)(message, handle)
    }
  } yield result

  override def handle[V](f: Message => Future[V]): Future[Unit] = {
    // Submitting to an ExecutionContext introduces an async boundary that reset the stack,
    // that makes Future behave like it's trampolining, so this function is stack safe.
    def loop: Future[List[V]] = handleN[V](10)(f).transformWith {
      case Success(_)  => loop
      case Failure(ex) =>
        logger.error(s"Error trying to consume a message from SQS - ${ex.getMessage}")
        loop
    }
    loop.map(_ => ())
  }

}
