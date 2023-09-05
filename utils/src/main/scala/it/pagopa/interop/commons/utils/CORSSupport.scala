package it.pagopa.interop.commons.utils

import akka.http.scaladsl.model.HttpMethods.{GET, OPTIONS, POST}
import akka.http.scaladsl.model.headers.{
  `Access-Control-Allow-Credentials`,
  `Access-Control-Allow-Headers`,
  `Access-Control-Allow-Methods`,
  `Access-Control-Allow-Origin`
}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, options, respondWithHeaders, _}
import akka.http.scaladsl.server.{Directive0, Route}

/** Exposes Akka HTTP directives for CORS features
  */
trait CORSSupport {

  /** Decorates the input route with the following CORS headers:<br><br>
    * <ul>
    *   <li><code>Access-Control-Allow-Origin: *</code></li>
    *   <li><code>Access-Control-Allow-Credentials: true</code></li>
    *   <li><code>Access-Control-Allow-Headers: Authorization, Content-Type, X-Requested-With</code></li>
    * </ul>
    * <br>
    * @param r - the route to decorate
    * @return - the decorated route
    */
  def corsHandler(r: Route): Route = addAccessControlHeaders { preflightRequestHandler ~ r }

  private def addAccessControlHeaders: Directive0 = respondWithHeaders(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With", "Accept-Language")
  )

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, GET)))
  }
}
