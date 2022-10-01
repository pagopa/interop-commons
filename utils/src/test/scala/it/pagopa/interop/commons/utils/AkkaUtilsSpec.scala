package it.pagopa.interop.commons.utils

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.Missing
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.errors.ComponentError
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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

    "return a bearer if contained" in {
      val contexts: Seq[(String, String)] = List((BEARER, "RoarerIAmABearer"))
      getBearer(contexts) shouldBe Right("RoarerIAmABearer")
      getFutureBearer(contexts).futureValue shouldBe "RoarerIAmABearer"
    }

    "return a 9999 error if doesn't contain bearer" in {
      val contexts: Seq[(String, String)] = List(("something_else", "WhereIsYoghi?"))
      getBearer(contexts) should matchPattern { case Left(x: ComponentError) if x.code == "9999" => }
      getFutureBearer(contexts).failed.futureValue should matchPattern {
        case x: ComponentError if x.code == "9999" =>
      }
    }

    "return an uid if contained" in {
      val contexts: Seq[(String, String)] = List((UID, "doone"))
      getUid(contexts) shouldBe Right("doone")
      getUidFuture(contexts).futureValue shouldBe "doone"
    }

    "return a 9996 error if doesn't contain uid" in {
      val contexts: Seq[(String, String)] = List(("something_else", "doone"))
      getUid(contexts) should matchPattern { case Left(x: ComponentError) if x.code == "9996" => }
      getUidFuture(contexts).failed.futureValue should matchPattern {
        case x: ComponentError if x.code == "9996" =>
      }
    }

    "return an sub if contained" in {
      val contexts: Seq[(String, String)] = List(("sub", "doone"))
      getSub(contexts) shouldBe Right("doone")
      getSubFuture(contexts).futureValue shouldBe "doone"
    }

    "return a 9995 error if doesn't contain uid" in {
      val contexts: Seq[(String, String)] = List(("something_else", "doone"))
      getSub(contexts) should matchPattern { case Left(x: ComponentError) if x.code == "9995" => }
      getSubFuture(contexts).failed.futureValue should matchPattern {
        case x: ComponentError if x.code == "9995" =>
      }
    }

    "return an custom claim if contained" in {
      val contexts: Seq[(String, String)] = List(("paperino", "doone"))
      getClaim(contexts, "paperino") shouldBe Right("doone")
      getClaimFuture(contexts, "paperino").futureValue shouldBe "doone"
    }

    "return a 9990 error if doesn't contain a custom claim" in {
      val contexts: Seq[(String, String)] = List(("something_else", "WhereIsYoghi?"))
      getClaim(contexts, "paperino") should matchPattern { case Left(x: ComponentError) if x.code == "9990" => }
      getClaimFuture(contexts, "paperino").failed.futureValue should matchPattern {
        case x: ComponentError if x.code == "9990" =>
      }
    }

  }

}
