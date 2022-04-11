package it.pagopa.interop.commons.queue.message

import spray.json.RootJsonFormat

trait JsonSerde[T] {
  val rootJsonFormat: RootJsonFormat[T]
}

object JsonSerde {
  def apply[T](implicit j: JsonSerde[T]): JsonSerde[T] = j
}
