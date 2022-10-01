package it.pagopa.interop.commons.ratelimiter.akkahttp

import akka.http.scaladsl.server.Rejection
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM

object Errors {
  object MissingOrganizationIdClaim
      extends Throwable(s"Missing expected $ORGANIZATION_ID_CLAIM claim in token")
      with Rejection

}
