package it.pagopa.interop.commons.cqrs.service

import cats.implicits._
import it.pagopa.interop.commons.cqrs.errors.ReadModelErrors.ReadModelMissingDataField
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.TypeConversions._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{ConnectionString, Document, MongoClient, MongoClientSettings}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

final class ReadModelService(dbConfig: ReadModelConfig) {

  private val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(dbConfig.connectionString))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  private val db = client.getDatabase(dbConfig.dbName)

  def findOne[T: JsonReader](collectionName: String, filter: Bson)(implicit ec: ExecutionContext): Future[Option[T]] =
    find[T](collectionName, filter, offset = 0, limit = 1).map(_.headOption)

  def find[T: JsonReader](collectionName: String, filter: Bson, offset: Int, limit: Int)(implicit
    ec: ExecutionContext
  ): Future[Seq[T]] =
    // TODO Nice to have: remove the execution context. This works but I don't like throwing the error
//    db
//      .getCollection(collectionName)
//      .find(filter)
//      .skip(offset)
//      .limit(limit)
//      .map(result => extractData[T](result).fold(ex => throw ex, identity))
//      .toFuture()
    for {
      results <- db
        .getCollection(collectionName)
        .find(filter)
        .skip(offset)
        .limit(limit)
        .toFuture()
      model   <- results.traverse(extractData[T](_).toFuture)
    } yield model

  def aggregate[T: JsonReader](collectionName: String, pipeline: Seq[Bson])(implicit
    ec: ExecutionContext
  ): Future[Seq[T]] =
    for {
      results <- db
        .getCollection(collectionName)
        .aggregate(pipeline)
        .toFuture()
      model   <- results.traverse(extractData[T](_).toFuture)
    } yield model

  private def extractData[T: JsonReader](document: Document): Either[Throwable, T] =
    document
      .toJson()
      .parseJson
      .asJsObject
      .fields
      .get("data")
      .toRight(ReadModelMissingDataField)
      .flatMap(data => Either.catchNonFatal(data.convertTo[T]))

}
