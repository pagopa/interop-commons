package it.pagopa.interop.commons.cqrs.service

import org.mongodb.scala.Document
import spray.json.{JsonWriter, _}

object DocumentConversions {

  implicit class SerializableToDocument[S: JsonWriter](v: S) extends AnyRef {
    def toDocument = Document(v.toJson.compactPrint)
  }
}
