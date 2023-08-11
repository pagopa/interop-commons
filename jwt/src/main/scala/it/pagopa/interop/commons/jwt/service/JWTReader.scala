package it.pagopa.interop.commons.jwt.service

import cats.syntax.all._
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{optionalHeaderValueByName, provide, reject, extractRequest}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, MalformedHeaderRejection}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.jwt.{getUserRoles, getExternalId}
import it.pagopa.interop.commons.utils.AkkaUtils.getBearer
import it.pagopa.interop.commons.utils.{
  BEARER,
  CORRELATION_ID_HEADER,
  ORGANIZATION_ID_CLAIM,
  SUB,
  UID,
  USER_ROLES,
  ORGANIZATION_EXTERNAL_ID_ORIGIN,
  ORGANIZATION_EXTERNAL_ID_VALUE
}

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
  ): Directive1[Seq[(String, String)]] = for {
    req       <- extractRequest
    ctx       <- optionalHeaderValueByName(CORRELATION_ID_HEADER).map(
      _.fold(Seq.empty[(String, String)])(cid => Seq(CORRELATION_ID_HEADER -> cid))
    )
    maybeAuth <- optionalHeaderValueByName("Authorization").map(_.map(_.split(" ").toList))
    res       <- maybeAuth match {
      case Some("Bearer" :: payload :: Nil) =>
        bearerAsContexts(payload) match {
          case Success(x)  => provide(x)
          case Failure(ex) =>
            logger.warn(s"Invalid authentication provided - ${ex.getMessage}")(ctx)
            reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", None)))
              .toDirective[Tuple1[Seq[(String, String)]]]
        }
      case Some(_)                          =>
        logger.warn(s"No authentication has been provided for this call ${req.method.value} ${req.uri}")(ctx)
        reject(MalformedHeaderRejection("Authorization", "Illegal header key."))
          .toDirective[Tuple1[Seq[(String, String)]]]
      case None                             =>
        logger.warn(s"No authentication has been provided for this call ${req.method.value} ${req.uri}")(ctx)
        reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", None)))
          .toDirective[Tuple1[Seq[(String, String)]]]
    }
  } yield res

  private def bearerAsContexts(bearer: String): Try[Seq[(String, String)]] = for {
    claims              <- getClaims(bearer)
    uid                 <- Try(Option(claims.getStringClaim(UID)).getOrElse(""))
    sub                 <- Try(Option(claims.getSubject).getOrElse(""))
    maybeOrganizationId <- Try(Option(claims.getStringClaim(ORGANIZATION_ID_CLAIM)))
    maybeExternalId = getExternalId(claims)
    userRoles       = getUserRoles(claims).mkString(",")
  } yield {
    val orgId: List[(String, String)]      = maybeOrganizationId.map(ORGANIZATION_ID_CLAIM -> _).toList
    val externalId: List[(String, String)] = maybeExternalId match {
      case Some((origin, value)) =>
        List(ORGANIZATION_EXTERNAL_ID_ORIGIN -> origin, ORGANIZATION_EXTERNAL_ID_VALUE -> value)
      case None                  => Nil
    }
    List(BEARER -> bearer, UID -> uid, SUB -> sub, USER_ROLES -> userRoles) ++ orgId ++ externalId
  }

}
