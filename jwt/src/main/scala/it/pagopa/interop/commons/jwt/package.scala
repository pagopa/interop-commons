package it.pagopa.interop.commons

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.utils._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try
import cats.syntax.all._
import java.{util => ju}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors._
import it.pagopa.interop.commons.utils.TypeConversions._
import com.nimbusds.jose.util.JSONObjectUtils

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

  def getUserRoles(claims: JWTClaimsSet): Set[String] = {
    val maybeRoles: Try[List[String]] = for {
      roles     <- getOrganizationRolesClaimSafe(claims)
      userRoles <- roles.traverse(getRoleSafe)
    } yield userRoles

    val roles: Set[String] = maybeRoles.fold(
      e => {
        logger.warn(s"Unable to extract userRoles from claims: ${e.getMessage()}")
        Set.empty[String]
      },
      _.toSet
    )

    getInteropRoleClaimSafe(claims).fold(roles)(roles + _)
  }

  def authorizeInterop[T](isAuthorized: => Boolean, errorMessage: => T)(
    route: Route
  )(implicit contexts: Seq[(String, String)], errorMarshaller: ToEntityMarshaller[T]): Route = if (isAuthorized) route
  else {
    val values: Map[String, String] = contexts.toMap
    val ipAddress: String           = values.getOrElse(IP_ADDRESS, "")
    val uid: String                 = values.get(UID).filterNot(_.isBlank).orElse(values.get(SUB)).getOrElse("")
    val correlationId: String       = values.getOrElse(CORRELATION_ID_HEADER, "")
    val header: String              = s"[IP=$ipAddress] [UID=$uid] [CID=$correlationId]"
    val body: String                = values
      .get(USER_ROLES)
      .fold(s"No user roles found to execute this request")(roles =>
        s"Invalid user roles ($roles) to execute this request"
      )

    logger.error(s"$header $body")
    complete(StatusCodes.Forbidden, errorMessage)
  }

}
