package it.pagopa.pdnd.interop.commons.jwt.service

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, MalformedHeaderRejection}
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, provide, reject}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.getBearer

import scala.util.{Failure, Success, Try}

/** Gets bearer token claims coming from the client
  */
trait JWTReader {

  private[this] lazy val BEARER = "bearer"

  /** Returns the claims contained in the JWT bore as <code>Authorization</code> HTTP header <code>bearer</code>.
    * @param bearer attribute representing the bore authorization header
    * @return claims contained in the bearer, if the JWT is valid
    */
  def getClaims(bearer: String): Try[JWTClaimsSet]

  /** Gets the bearer string from headers and checks if it contains a valid JWT.
    * @param contexts - HTTP headers contexts
    * @return the bearer token, if it contains a valid JWT
    */
  def parseValidBearer(contexts: Seq[(String, String)]): Try[String] =
    for {
      bearer <- getBearer(contexts)
      _      <- getClaims(bearer)
    } yield bearer

  /** Gets the bearer string from headers and checks if it contains a valid JWT.
    * @param bearer - HTTP bearer
    * @return the bearer token, if it contains a valid JWT
    */
  def parseValidBearer(bearer: String): Try[String] =
    for {
      _ <- getClaims(bearer)
    } yield bearer

  /** Returns a directive containing the request contexts as sequence.
    * If not valid bearer is provided, it propagates an <code>AuthenticationFailedRejection</code>
    * @return contexts as sequence of pairs
    */
  def contextsJWTValidator: Directive1[Seq[(String, String)]] = {
    def bearerAsContexts(bearer: String) =
      for {
        _ <- getClaims(bearer)
      } yield Seq(BEARER -> bearer)

    validationDirective(bearerAsContexts)
  }

  /** Returns a directive containing the JWT claims set
    * If not valid bearer is provided, it propagates an <code>AuthenticationFailedRejection</code>
    * @return JWT claims set
    */
  def claimsJWTValidator: Directive1[JWTClaimsSet] = {
    validationDirective(getClaims)
  }

  private def validationDirective[T](validation: String => Try[T]): Directive1[T] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(header) =>
        header.split(" ").toList match {
          case "Bearer" :: payload :: Nil =>
            validation.andThen(validationAsDirective)(payload)
          case _ =>
            reject(MalformedHeaderRejection("Authorization", "Illegal header key."))
        }
      case None =>
        reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", None)))
    }

  private def validationAsDirective[T]: Try[T] => Directive1[T] = { validation =>
    validation match {
      case Success(result) => provide(result)
      case Failure(_) =>
        reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", None)))
    }
  }
}
