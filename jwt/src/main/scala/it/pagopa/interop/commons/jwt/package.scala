package it.pagopa.interop.commons

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import cats.syntax.all._
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors._
import it.pagopa.interop.commons.utils.errors.{AkkaResponses, ServiceCode}
import it.pagopa.interop.commons.utils._
import org.slf4j.{Logger, LoggerFactory}

import java.{util => ju}
import scala.util.Try
import scala.util.Success
import scala.util.Failure

package object jwt {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  type KID           = String
  type SerializedKey = String

  final val clientIdClaim     = "client_id"
  final val typClaim          = "typ"
  final val roleClaim         = "role"
  final val organizationClaim = "organization"

  final val ADMIN_ROLE    = "admin"
  final val SECURITY_ROLE = "security"
  final val API_ROLE      = "api"
  final val M2M_ROLE      = "m2m"
  final val INTERNAL_ROLE = "internal"
  final val SUPPORT_ROLE  = "support"

  final val M2M_ROLES      = Map("role" -> M2M_ROLE)
  final val INTERNAL_ROLES = Map("role" -> INTERNAL_ROLE)

  private[jwt] def rsaVerifier(jwkKey: String): Try[RSASSAVerifier] = Try {
    val jwk: JWK  = JWK.parse(jwkKey)
    val publicKey = jwk.toRSAKey
    new RSASSAVerifier(publicKey)
  }

  private[jwt] def ecVerifier(jwkKey: String): Try[ECDSAVerifier] = Try {
    val jwk: JWK  = JWK.parse(jwkKey)
    val publicKey = jwk.toECKey
    new ECDSAVerifier(publicKey)
  }

  /**
    * Checks if the implicit contexts of a request contain at least one admittable role
    * @param admittedRoles roles admitted for this request
    * @param contexts request context attributes
    * @return true if at least an admittable roles exist for this request, false otherwise
    */
  def hasPermissions(admittedRoles: String*)(implicit contexts: Seq[(String, String)]): Boolean =
    admittedRoles.distinct match {
      case Nil => false
      case x   =>
        val requestRoles: Array[String] = contexts.toMap.get(USER_ROLES).map(_.split(",")).getOrElse(Array.empty)
        x.intersect(requestRoles).size > 0
    }

  private def getJSONObjectClaimsSafe(claims: JWTClaimsSet): Try[ju.Map[String, Object]] =
    Try(claims.getJSONObjectClaim(organizationClaim))
      .as(MissingClaim(organizationClaim))
      .flatMap(nullable => Option(nullable).toTry(MissingClaim(organizationClaim)))

  private def getRolesSafe(map: ju.Map[String, Object]): Try[List[ju.Map[String, Object]]] =
    Try(JSONObjectUtils.getJSONObjectArray(map, "roles"))
      .as(GenericError("Roles in context are not in json format"))
      .flatMap(nullable => Option(nullable).map(_.toList).toTry(MissingClaim("roles in organization")))

  private def getOrganizationRolesClaimSafe(claims: JWTClaimsSet): Try[List[ju.Map[String, Object]]] =
    getJSONObjectClaimsSafe(claims) >>= getRolesSafe

  private def getRoleSafe(map: ju.Map[String, Object]): Try[String] =
    Try(JSONObjectUtils.getString(map, roleClaim))
      .flatMap(nullable => Option(nullable).toTry(GenericError("Roles in context are not in json format")))
      .as(GenericError("Roles in context are not in json format"))

  private def getInteropRoleClaimSafe(claims: JWTClaimsSet): Option[String] =
    Try(claims.getStringClaim(roleClaim)).toOption.flatMap(Option(_))

  private[jwt] def getExternalId(claims: JWTClaimsSet): Option[(String, String)] = {
    def externalIdFromOrganizationClaim(): Try[(String, String)] = for {
      externalIdFromClaim <- Try(claims.getJSONObjectClaim(ORGANIZATION_EXTERNAL_ID_CLAIM))
        .flatMap(nullable => Option(nullable).toTry(MissingClaim(ORGANIZATION_EXTERNAL_ID_CLAIM)))
      origin              <- Try(externalIdFromClaim.get(ORGANIZATION_EXTERNAL_ID_ORIGIN_CLAIM))
        .flatMap(nullable =>
          Option(nullable)
            .toTry(MissingClaim(s"$ORGANIZATION_EXTERNAL_ID_CLAIM.$ORGANIZATION_EXTERNAL_ID_ORIGIN_CLAIM"))
            .map(_.toString)
        )
      value               <- Try(externalIdFromClaim.get(ORGANIZATION_EXTERNAL_ID_VALUE_CLAIM))
        .flatMap(nullable =>
          Option(nullable)
            .toTry(MissingClaim(s"$ORGANIZATION_EXTERNAL_ID_CLAIM.$ORGANIZATION_EXTERNAL_ID_VALUE_CLAIM"))
            .map(_.toString)
        )
    } yield (origin, value)

    externalIdFromOrganizationClaim() match {
      case Success((origin, value)) => Some((origin, value))
      case Failure(_)               => None
    }
  }

  def getUserRoles(claims: JWTClaimsSet): Set[String] = {

    val rolesFromInteropClaim: Try[List[String]] = Try(claims.getStringClaim("role"))
      .flatMap(nullable => Option(nullable).toTry(GenericError("User roles in context are not in valid format")))
      .map(roles => roles.split(",").toList)

    val userRolesStringFromInteropClaim: Try[List[String]] = Try(claims.getStringClaim(USER_ROLES))
      .flatMap(nullable => Option(nullable).toTry(GenericError("User roles in context are not in valid format")))
      .map(roles => roles.split(",").toList)

    def userRolesStringFromOrganizationClaim(): Try[List[String]] = for {
      roles     <- getOrganizationRolesClaimSafe(claims)
      userRoles <- roles.traverse(getRoleSafe)
    } yield userRoles

    val roles: Set[String] = rolesFromInteropClaim
      .orElse(userRolesStringFromInteropClaim)
      .orElse[List[String]](userRolesStringFromOrganizationClaim())
      .fold(
        e => {
          logger.warn(s"Unable to extract userRoles from claims: ${e.getMessage()}")
          Set.empty[String]
        },
        _.toSet
      )

    getInteropRoleClaimSafe(claims).fold(roles)(roles + _)
  }

  def authorize(roles: String*)(route: => Route)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    serviceCode: ServiceCode
  ): Route =
    if (hasPermissions(roles: _*)) route
    else {
      val logMessage: String = contexts.toMap
        .get(USER_ROLES)
        .fold(s"No user roles found to execute this request")(roles =>
          s"Invalid user roles ($roles) to execute this request"
        )

      AkkaResponses.forbidden(OperationForbidden, logMessage)
    }

  // TODO Kept for backward compatibility.
  //      To be removed as soon as all services have been migrated to the new authorize function
  def authorizeInterop[T](isAuthorized: => Boolean, errorMessage: => T)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], errorMarshaller: ToEntityMarshaller[T]): Route = if (isAuthorized) route
  else {
    val values: Map[String, String] = contexts.toMap
    val ipAddress: String           = values.getOrElse(IP_ADDRESS, "")
    val uid: String                 = values.get(UID).filterNot(_.isBlank).orElse(values.get(SUB)).getOrElse("")
    val organizationId: String      = values.getOrElse(ORGANIZATION_ID_CLAIM, "")
    val correlationId: String       = values.getOrElse(CORRELATION_ID_HEADER, "")
    val header: String              = s"[IP=$ipAddress] [UID=$uid] [OID=$organizationId] [CID=$correlationId]"
    val body: String                = values
      .get(USER_ROLES)
      .fold(s"No user roles found to execute this request")(roles =>
        s"Invalid user roles ($roles) to execute this request"
      )

    logger.error(s"$header $body")
    complete(StatusCodes.Forbidden, errorMessage)
  }
}
