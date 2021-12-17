package it.pagopa.pdnd.interop.commons.jwt.service

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, provide, reject, extractUri}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, MalformedHeaderRejection}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.getBearer
import it.pagopa.pdnd.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER, UID}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/** Gets bearer token claims coming from the client
  */
trait JWTReader {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the claims contained in the JWT bore as <code>Authorization</code> HTTP header <code>bearer</code>.
    *
    * @param bearer attribute representing the bore authorization header
    * @return claims contained in the bearer, if the JWT is valid
    */
  def getClaims(bearer: String): Try[JWTClaimsSet]

  /** Gets the bearer string from headers and checks if it contains a valid JWT.
    *
    * @param contexts - HTTP headers contexts
    * @return the bearer token, if it contains a valid JWT
    */
  def parseValidBearer(contexts: Seq[(String, String)]): Try[String] =
    for {
      bearer <- getBearer(contexts)
      _      <- getClaims(bearer)
    } yield bearer

  /** Gets the bearer string from headers and checks if it contains a valid JWT.
    *
    * @param bearer - HTTP bearer
    * @return the bearer token, if it contains a valid JWT
    */
  def parseValidBearer(bearer: String): Try[String] =
    for {
      _ <- getClaims(bearer)
    } yield bearer

  /** Returns a directive containing the request contexts as sequence.
    * If not valid bearer is provided, it propagates an <code>AuthenticationFailedRejection</code>
    *
    * @return contexts as sequence of pairs
    */
  def OAuth2JWTValidatorAsContexts: Directive1[Seq[(String, String)]] = {
    def bearerAsContexts(bearer: String) =
      for {
        claims <- getClaims(bearer)
        uid <- Try {
          claims.getStringClaim(UID)
        }
      } yield Seq(BEARER -> bearer, UID -> Option(uid).getOrElse(""))

    authenticationDirective(bearerAsContexts)
  }

  /** Returns a directive containing the JWT claims set
    * If not valid bearer is provided, it propagates an <code>AuthenticationFailedRejection</code>
    *
    * @return JWT claims set
    */
  def OAuth2JWTValidatorAsClaimsSet: Directive1[JWTClaimsSet] = {
    authenticationDirective(getClaims)
  }

  private def authenticationDirective[T](validation: String => Try[T]): Directive1[T] = {
    extractUri.flatMap { uri =>
      optionalHeaderValueByName(CORRELATION_ID_HEADER).flatMap { optCorrelationId =>
        val contextInfo = s"$uri - [${optCorrelationId.getOrElse("")}]"
        optionalHeaderValueByName("Authorization").flatMap {
          case Some(header) =>
            header.split(" ").toList match {
              case "Bearer" :: payload :: Nil =>
                validation.andThen(authenticationDirective(contextInfo))(payload)
              case _ =>
                logger.error(s"$contextInfo - No authentication has been provided for this call")
                reject(MalformedHeaderRejection("Authorization", "Illegal header key."))
            }
          case None =>
            logger.error(s"$contextInfo - No authentication has been provided for this call")
            reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", None)))
        }
      }
    }
  }

  private def authenticationDirective[T](contextInfo: String): Try[T] => Directive1[T] = { validation =>
    validation match {
      case Success(result) => provide(result)
      case Failure(_) =>
        logger.error(s"$contextInfo - Invalid authentication provided")
        reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", None)))
    }
  }
}
