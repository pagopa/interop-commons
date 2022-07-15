package it.pagopa.interop.commons.cqrs.service

import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.slick.SlickProjection
import it.pagopa.interop.commons.cqrs.model.{MongoDbConfig, PartialMongoAction}
import it.pagopa.interop.commons.cqrs.service.CqrsProjection.EventHandler
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

final case class CqrsProjection[T](
  offsetDbConfig: DatabaseConfig[JdbcProfile],
  mongoDbConfig: MongoDbConfig,
  projectionId: String,
  eventHandler: EventHandler[T]
)(implicit system: ActorSystem[_], ec: ExecutionContext) {

  private implicit val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(mongoDbConfig.connectionString))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[T]] =
    EventSourcedProvider
      .eventsByTag[T](system, readJournalPluginId = JdbcReadJournal.Identifier, tag = tag)

  def projection(tag: String): ExactlyOnceProjection[Offset, EventEnvelope[T]] = SlickProjection.exactlyOnce(
    projectionId = ProjectionId(projectionId, tag),
    sourceProvider = sourceProvider(tag),
    handler = () => CqrsProjectionHandler(eventHandler, mongoDbConfig.dbName, mongoDbConfig.collectionName),
    databaseConfig = offsetDbConfig
  )
}

object CqrsProjection {
  type EventHandler[T] = (MongoCollection[Document], T) => PartialMongoAction
}
