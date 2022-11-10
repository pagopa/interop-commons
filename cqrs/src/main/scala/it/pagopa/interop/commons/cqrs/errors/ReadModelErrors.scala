package it.pagopa.interop.commons.cqrs.errors

object ReadModelErrors {

  object ReadModelMissingDataField
      extends Throwable(s"Error retrieving data from read-model: Missing field 'data' in document")
}
