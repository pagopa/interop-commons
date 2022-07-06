package it.pagopa.interop.commons.cqrs.model

import org.mongodb.scala.{Document, SingleObservable}
import org.mongodb.scala.bson.conversions.Bson

/**
  * The partial action is used to intercept and enhance parameters passed to the mongo command.
  * Example: Adding metadata to documents saved and updated
  */
trait PartialMongoAction

/**
  * Used when the value parameter is of type Bson, for example when updating using functions like Updates.set or Updates.pull
  * 
  * Usage:
  * {{{
  * ActionWithBson(
  *   value => collection.updateOne(Filters.eq("data.id", "xyz"), value), 
  *   Updates.set("data", "data")
  * )  
  * }}}
  */
final case class ActionWithBson(action: Bson => SingleObservable[_], value: Bson) extends PartialMongoAction

/**
  * Used when the value parameter is of type Document, for example when inserting a new document
  *
  * Usage:
  * {{{
  * ActionWithDocument(
  *   value => collection.insertOne(value), 
  *   Document(s"{ data: 123 }")
  * )
  * }}}
  */
final case class ActionWithDocument(action: Document => SingleObservable[_], doc: Document) extends PartialMongoAction

/**
  * Used when there is no value parameter, for example when deleting a document
  *
  * Usage:
  * {{{
  * Action(collection.deleteOne(Filters.eq("data.id", "xyz")))
  * }}}
  */
final case class Action(action: SingleObservable[_]) extends PartialMongoAction
