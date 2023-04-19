package it.pagopa.interop.commons.cqrs.model

import org.mongodb.scala.{Document, Observable, SingleObservable}
import org.mongodb.scala.bson.conversions.Bson

/**
  * The partial action is used to intercept and enhance parameters passed to the mongo command.
  * Example: Adding metadata to documents saved and updated
  */
sealed trait PartialMongoAction

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
  * Used when there is no value parameter and metadata should not be added, for example when deleting a document
  *
  * Usage:
  * {{{
  * Action(collection.deleteOne(Filters.eq("data.id", "xyz")))
  * }}}
  */
final case class Action(action: SingleObservable[_]) extends PartialMongoAction

/**
  * Used when the value parameter is the result of a previous operation
  *
  * Usage:
  * {{{
  * val result: Observable[...] = collection.find(...)
  *
  * ActionWithObservable(
  *   value => collection.updateOne(Filters.eq("data.id", "xyz"), value),
  *   result
  * )
  * }}}
  */
final case class ActionWithObservable[T](action: Bson => SingleObservable[T], observable: Observable[Bson])
    extends PartialMongoAction

/**
  * Used when more than one action is required
  * Note: there is not guarantee that actions are executed sequentially
  *
  * Usage:
  * {{{
  * MultiAction(
  *   Seq(
  *     Action(collection.deleteOne(Filters.eq("data.id", "xyz")),
  *     ActionWithDocument(value => collection.insertOne(value), Document(s"{ data: 123 }"))
  *   )
  * )
  * }}}
  */
final case class MultiAction(actions: Seq[PartialMongoAction]) extends PartialMongoAction

final case class ErrorAction(error: Throwable) extends PartialMongoAction

/**
  * Used when no action is required
  */
object NoOpAction extends PartialMongoAction
