package it.pagopa.interop.commons.jwt.service

import cats.syntax.all._
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{extractUri, optionalHeaderValueByName, provide, reject}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, MalformedHeaderRejection}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.jwt.getUserRoles
import it.pagopa.interop.commons.utils.AkkaUtils.getBearer
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER, ORGANIZATION_ID_CLAIM, SUB, UID, USER_ROLES}

import scala.util.{Failure, Success, Try}
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog

trait JWTReader {

  def getClaims(bearer: String): Try[JWTClaimsSet]

  def parseValidBearer(contexts: Seq[(String, String)]): Try[String] =
    getBearer(contexts).toTry.flatMap(bearer => getClaims(bearer).as(bearer))

  def parseValidBearer(bearer: String): Try[String] = getClaims(bearer).as(bearer)

  def OAuth2JWTValidatorAsContexts(implicit
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Directive1[Seq[(String, String)]] = authenticationDirective(bearerAsContexts)

  private def bearerAsContexts(bearer: String): Try[List[(String, String)]] = for {
    claims         <- getClaims(bearer)
    uid            <- Try(Option(claims.getStringClaim(UID)).getOrElse(""))
    sub            <- Try(Option(claims.getSubject).getOrElse(""))
    organizationId <- Try(claims.getStringClaim(ORGANIZATION_ID_CLAIM))
    userRoles = getUserRoles(claims).mkString(",")
  } yield {
    val orgId: List[(String, String)] = Option(organizationId).map(ORGANIZATION_ID_CLAIM -> _).toList
    List(BEARER -> bearer, UID -> uid, SUB -> sub, USER_ROLES -> userRoles) ++ orgId
  }

  private def authenticationDirective[T](
    validation: String => Try[T]
  )(implicit logger: LoggerTakingImplicit[ContextFieldsToLog]): Directive1[T] = {
    // for {
    //   uri       <- extractUri
    //   maybeCid  <- optionalHeaderValueByName(CORRELATION_ID_HEADER)
    //   maybeAuth <- optionalHeaderValueByName("Authorization")
    // } yield {
    //   implicit val cftl: ContextFieldsToLog = maybeCid.fold(List.empty)(cid => ("cid" -> cid) :: Nil)
    //   maybeAuth.fold {
    //     logger.warn("No authentication has been provided for this call")
    //     reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", None)))
    //   }
    // }

    extractUri.flatMap { _ =>
      optionalHeaderValueByName(CORRELATION_ID_HEADER).flatMap { maybeCid =>
        implicit val cftl: ContextFieldsToLog = maybeCid.fold(List.empty[(String, String)])(cid => List("cid" -> cid))
        optionalHeaderValueByName("Authorization").flatMap {
          case Some(header) =>
            header.split(" ").toList match {
              case "Bearer" :: payload :: Nil =>
                validation.andThen(authenticationDirective)(payload)
              case _                          =>
                logger.warn(s"No authentication has been provided for this call")
                reject(MalformedHeaderRejection("Authorization", "Illegal header key."))
            }
          case None         =>
            logger.warn(s"No authentication has been provided for this call")
            reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", None)))
        }
      }
    }
  }

  private def authenticationDirective[T]: Try[T] => Directive1[T] = {
    case Success(result) => provide(result)
    case Failure(_)      =>
      // logger.warn(s"Invalid authentication provided - ${ex.getMessage}")
      reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", None)))
  }
}
