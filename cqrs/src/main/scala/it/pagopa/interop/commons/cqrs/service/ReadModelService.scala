package it.pagopa.interop.commons.cqrs.service

import cats.implicits._
import it.pagopa.interop.commons.cqrs.model.MongoDbConfig
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{ConnectionString, Document, MongoClient, MongoClientSettings}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

final class ReadModelService(mongoDbConfig: MongoDbConfig) {

  private implicit val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(mongoDbConfig.connectionString))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  def findOne[T: JsonReader](filter: Bson)(implicit ec: ExecutionContext): Future[Option[T]] =
    find[T](filter, offset = 0, limit = 1).map(_.headOption)

  def find[T: JsonReader](filter: Bson, offset: Int, limit: Int)(implicit ec: ExecutionContext): Future[Seq[T]] =
    // TODO Nice to have: remove the execution context
//      client
//        .getDatabase(mongoDbConfig.dbName)
//        .getCollection(mongoDbConfig.collectionName)
//        .find(filter).map(extractData[T]).toFuture
//    OR
//    client
//      .getDatabase(mongoDbConfig.dbName)
//      .getCollection(mongoDbConfig.collectionName)
//      .find(filter)
//      .map(result =>
//        extractData[T](result) match {
//          case Success(r)  => Observable(List(r))
////            Future.successful(r)
//          case Failure(ex) =>
//
//            Future.failed(ex)
//        }
//      )

    for {
      results <- client
        .getDatabase(mongoDbConfig.dbName)
        .getCollection(mongoDbConfig.collectionName)
        .find(filter)
        .skip(offset)
        .limit(limit)
        .toFuture()
      model   <- results.traverse(extractData[T](_).toFuture)
    } yield model

  private def extractData[T: JsonReader](document: Document): Try[T] = for {
    fields <- Try(document.toJson().parseJson.asJsObject.getFields("data")).leftMap(ex =>
      // TODO Use proper exception
      new Exception(s"Error retrieving data from read-model: Unable to extract field 'data'. Reason: ${ex.getMessage}")
    )
    result <- fields match {
      case data :: Nil => Try(data.convertTo[T])
      case _           =>
        // TODO Use proper exception
        Failure(
          new Exception(
            s"Error retrieving data from read-model: Unexpected number of fields ${fields.size}. Content: $fields"
          )
        )
    }
  } yield result

}
