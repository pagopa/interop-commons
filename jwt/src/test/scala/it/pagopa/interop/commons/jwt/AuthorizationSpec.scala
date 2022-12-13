package it.pagopa.interop.commons.jwt

import akka.http.scaladsl.server.Directives.{complete, get}
import akka.http.scaladsl.server.Route
import it.pagopa.interop.commons.utils.USER_ROLES
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.errors.ServiceCode

import scala.annotation.nowarn

class AuthorizationSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
  implicit val serviceCode: ServiceCode                         = ServiceCode("000")

  "Authorization method should be lazy and not consume the route definition eagerly" in {
    implicit val contexts: List[(String, String)] = List()

    @nowarn
    def route: Route = {
      failTest("authorize is no more lazy")
      get { complete("OK") }
    }

    val routeToTest: Route = authorize("role1")(route)

    Get() ~> routeToTest ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }

  "Roles authorization" should {
    val okRoute = get { complete("OK") }

    "return Forbidden if no admittable roles are defined" in {
      implicit val contexts: List[(String, String)] = List(USER_ROLES -> "a,b,c")

      Get() ~> authorize()(okRoute) ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "return Forbidden when there is no match between admittable roles and provided ones" in {
      implicit val contexts: List[(String, String)] = List(USER_ROLES -> "a,b,c")

      Get() ~> authorize("admin", "operator", "api")(okRoute) ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "return the given route when there is a match between admittable roles and provided ones" in {
      hasPermissions("hello", "admin")(Seq(USER_ROLES -> "admin,operator,api")) shouldBe true

      implicit val contexts: List[(String, String)] = List(USER_ROLES -> "admin,operator,api")

      Get() ~> authorize("hello", "admin")(okRoute) ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
