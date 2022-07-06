package it.pagopa.interop.commons.cqrs.model

final case class MongoDbConfig(connectionString: String, dbName: String, collectionName: String)
