package it.pagopa.interop.commons.cqrs.service

import cats.implicits._
import it.pagopa.interop.commons.cqrs.errors.ReadModelErrors.ReadModelMissingDataField
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.TypeConversions._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Aggregates, Projections}
import org.mongodb.scala.{ConnectionString, Document, MongoClient, MongoClientSettings, MongoDatabase}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait ReadModelService {
  def findOne[T: JsonReader](collectionName: String, filter: Bson)(implicit ec: ExecutionContext): Future[Option[T]]
  def find[T: JsonReader](collectionName: String, filter: Bson, offset: Int, limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[T]]
  def find[T: JsonReader](collectionName: String, filter: Bson, projection: Bson, offset: Int, limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[T]]
  def aggregate[T: JsonReader](
    collectionName: String,
    pipeline: Seq[Bson],
    offset: Int,
    limit: Int,
    allowDiskUse: Boolean = false
  )(implicit ec: ExecutionContext): Future[Seq[T]]
  def aggregateRaw[T: JsonReader](
    collectionName: String,
    pipeline: Seq[Bson],
    offset: Int,
    limit: Int,
    allowDiskUse: Boolean = false
  )(implicit ec: ExecutionContext): Future[Seq[T]]
  def close(): Unit
}

final class MongoDbReadModelService(dbConfig: ReadModelConfig) extends ReadModelService {

  private val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(dbConfig.connectionString))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .build()
  )

  def close(): Unit = client.close()

  private val db: MongoDatabase = client.getDatabase(dbConfig.dbName)

  def findOne[T: JsonReader](collectionName: String, filter: Bson)(implicit ec: ExecutionContext): Future[Option[T]] =
    find[T](collectionName, filter, offset = 0, limit = 1).map(_.headOption)

  def find[T: JsonReader](collectionName: String, filter: Bson, offset: Int, limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[T]] = find[T](collectionName, filter, Projections.include(), offset, limit)

  def find[T: JsonReader](collectionName: String, filter: Bson, projection: Bson, offset: Int, limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[T]] =
    for {
      results <- db
        .getCollection(collectionName)
        .find(filter)
        .projection(projection)
        .skip(offset)
        .limit(limit)
        .toFuture()
      model   <- results.traverse(extractData[T](_).toFuture)
    } yield model

  def aggregate[T: JsonReader](
    collectionName: String,
    pipeline: Seq[Bson],
    offset: Int,
    limit: Int,
    allowDiskUse: Boolean = false
  )(implicit ec: ExecutionContext): Future[Seq[T]] =
    aggregator(collectionName, pipeline, offset, limit, allowDiskUse)(extractData[T])

  def aggregateRaw[T: JsonReader](
    collectionName: String,
    pipeline: Seq[Bson],
    offset: Int,
    limit: Int,
    allowDiskUse: Boolean = false
  )(implicit ec: ExecutionContext): Future[Seq[T]] =
    aggregator(collectionName, pipeline, offset, limit, allowDiskUse)(extract[T])

  private def aggregator[T](
    collectionName: String,
    pipeline: Seq[Bson],
    offset: Int,
    limit: Int,
    allowDiskUse: Boolean
  )(f: Document => Either[Throwable, T])(implicit ec: ExecutionContext) = db
    .getCollection(collectionName)
    .aggregate(pipeline ++ Seq(Aggregates.skip(offset), Aggregates.limit(limit)))
    .allowDiskUse(allowDiskUse)
    .toFuture()
    .flatMap(Future.traverse(_)(f(_).toFuture))

  private def extractData[T: JsonReader](document: Document): Either[Throwable, T] = document
    .toJson()
    .parseJson
    .asJsObject
    .fields
    .get("data")
    .toRight(ReadModelMissingDataField)
    .flatMap(data => Either.catchNonFatal(data.convertTo[T]))

  private def extract[T: JsonReader](document: Document): Either[Throwable, T] = Either
    .catchNonFatal(document.toJson().parseJson.asJsObject)
    .flatMap(data => Either.catchNonFatal(data.convertTo[T]))

}
