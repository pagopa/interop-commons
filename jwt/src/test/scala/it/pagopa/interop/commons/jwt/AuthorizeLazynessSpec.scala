package it.pagopa.interop.commons.jwt

import it.pagopa.interop.commons.jwt._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.StatusCodes
import scala.annotation.nowarn

class AuthorizeLazynessSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  "Authorization method should be lazy and not consume the route definition eagerly" in {
    implicit val contexts: List[(String, String)] = List()

    @nowarn
    def route: Route = {
      failTest("authorizeInterop is no more lazy")
      get { complete("OK") }
    }

    val routeToTest: Route = authorizeInterop[String](false, "You should see this")(route)

    Get() ~> routeToTest ~> check {
      status shouldBe StatusCodes.Forbidden
      entityAs[String] shouldBe "You should see this"
    }
  }
}
