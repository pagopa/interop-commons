package it.pagopa.pdnd.interop.commons.utils

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.Missing
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.{Authenticator, PassThroughAuthenticator}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.pdnd.interop.commons.utils.{BEARER, UID}
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils._
import scala.util.{Failure, Success}
import it.pagopa.pdnd.interop.commons.utils.errors.GenericComponentErrors
import org.scalatest.concurrent.ScalaFutures

class AkkaUtilsSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "an Authenticator" should {
    "retrieve the bearer token from the input credentials" in {
      val bearerToken              = "12345ahaaihak"
      val credentials: Credentials = Credentials(Some(OAuth2BearerToken(bearerToken)))
      Authenticator.apply(credentials) shouldBe Some(Seq("bearer" -> bearerToken))
    }

    "retrieve no token from missing input credentials" in {
      val credentials: Credentials = Missing
      Authenticator.apply(credentials) shouldBe None
    }
  }

  "a PassThroughAuthenticator" should {
    "return an empty sequence of contexts when credentials are set" in {
      val bearerToken              = "12345ahaaihak"
      val credentials: Credentials = Credentials(Some(OAuth2BearerToken(bearerToken)))
      PassThroughAuthenticator.apply(credentials) shouldBe Some(Seq.empty)
    }

    "retrieve an empty sequence of contexts when no credentials are set" in {
      val credentials: Credentials = Missing
      PassThroughAuthenticator.apply(credentials) shouldBe Some(Seq.empty)
    }
  }

  "a Context" should {
    "return an uid if contained" in {
      val contexts: Seq[(String, String)] = List((UID, "doone"))
      getUid(contexts) shouldBe Success("doone")
      getUidFuture(contexts).futureValue shouldBe "doone"
    }

    "return a bearer if contained" in {
      val contexts: Seq[(String, String)] = List((BEARER, "RoarerIAmABearer"))
      getBearer(contexts) shouldBe Success("RoarerIAmABearer")
      getFutureBearer(contexts).futureValue shouldBe "RoarerIAmABearer"
    }

    "return a 9996 error if doesn't contain uid" in {
      val contexts: Seq[(String, String)] = List((BEARER, "doone"))
      getUid(contexts) shouldBe Failure(GenericComponentErrors.MissingUid)
      getUidFuture(contexts).failed.futureValue shouldBe GenericComponentErrors.MissingUid

    }

    "return a 9999 error if doesn't contain bearer" in {
      val contexts: Seq[(String, String)] = List((UID, "WhereIsYoghi?"))
      getBearer(contexts) shouldBe Failure(GenericComponentErrors.MissingBearer)
      getFutureBearer(contexts).failed.futureValue shouldBe GenericComponentErrors.MissingBearer
    }
  }

}
