package it.pagopa.pdnd.interop.commons.utils

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.Missing
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.{Authenticator, PassThroughAuthenticator}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaUtilsSpec extends AnyWordSpecLike with Matchers {

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

}
