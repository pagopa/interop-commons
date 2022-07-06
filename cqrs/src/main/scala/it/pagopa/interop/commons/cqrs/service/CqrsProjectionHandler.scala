package it.pagopa.interop.commons.cqrs.service

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.slick.SlickHandler
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.cqrs.model._
import it.pagopa.interop.commons.cqrs.service.CqrsProjection.EventHandler
import it.pagopa.interop.commons.cqrs.service.DocumentConversions._
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import slick.dbio._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private[service] final case class CqrsProjectionHandler[T](
  eventHandler: EventHandler[T],
  dbName: String,
  collectionName: String
)(implicit ec: ExecutionContext, client: MongoClient)
    extends SlickHandler[EventEnvelope[T]] {

  private val logger: Logger = Logger(this.getClass)

  val collection: MongoCollection[Document] = client.getDatabase(dbName).getCollection(collectionName)

  // Note: the implementation is not idempotent
  override def process(envelope: EventEnvelope[T]): DBIO[Done] = DBIOAction.from {
    logger.debug(s"CQRS Projection: writing event with envelop $envelope")

    val metadata: CqrsMetadata = CqrsMetadata(sourceEvent =
      SourceEvent(
        persistenceId = envelope.persistenceId,
        sequenceNr = envelope.sequenceNr,
        timestamp = envelope.timestamp
      )
    )

    def withMetadata(op: Bson): Bson = Updates.combine(Updates.set("metadata", metadata.toDocument), op)

    val partialApplication = eventHandler(collection, envelope.event)
    val result             = partialApplication match {
      case ActionWithBson(action, value)   => action(withMetadata(value))
      case Action(action)                  => action
      case ActionWithDocument(action, doc) =>
        val metadataDocument = Document(s"{ metadata : ${metadata.toDocument.toJson()} }")
        val newDoc           = doc.concat(metadataDocument)
        action(newDoc)
    }

    val futureResult = result.toFuture()

    futureResult.onComplete {
      case Failure(e) => logger.error(s"Error on CQRS sink for ${show(metadata)}", e)
      case Success(_) => logger.debug(s"CQRS sink completed for ${show(metadata)}")
    }
    futureResult.as(Done)
  }

  private def show(metadata: CqrsMetadata): String =
    s"(persistenceId: ${metadata.sourceEvent.persistenceId}, sequenceNr: ${metadata.sourceEvent.sequenceNr}, timestamp : ${metadata.sourceEvent.timestamp})"

}
