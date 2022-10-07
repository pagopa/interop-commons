package it.pagopa.interop.commons

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.shaded.gson.{JsonArray, JsonObject}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.utils.USER_ROLES
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import scala.util.{Try, Failure, Success}
import cats.syntax.all._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors._
import it.pagopa.interop.commons.utils.TypeConversions._

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

  // Sadly getJSONObjectClaim both throws and returns null, so it requires a lot of handling
  private def getOrganizationRolesClaimSafe(claims: JWTClaimsSet): Try[JsonArray] = for {
    nullableOrgClaimsMap <- Try(claims.getJSONObjectClaim(organizationClaim)).as(MissingClaim(organizationClaim))
    orgClaims            <- Option(nullableOrgClaimsMap).toTry(MissingClaim(organizationClaim))
    orgClaimsMap = orgClaims.asScala.toMap
    roles <- orgClaimsMap.get("roles").toTry(MissingClaim("roles in organization"))
    _ = println(s"roles are ${roles.asInstanceOf[java.util.ArrayList[_]].get(0).getClass().getName()}")
    rolesJsonArray <- Try(roles.asInstanceOf[JsonArray]).as(GenericError("Roles in context are not in json format"))
  } yield rolesJsonArray

  private def getInteropRoleClaimSafe(claims: JWTClaimsSet): Option[String] =
    Try(claims.getStringClaim(roleClaim)).toOption.flatMap(Option(_))

  def getUserRoles(claims: JWTClaimsSet): Set[String] = {
    val maybeRoles: Try[List[String]] = for {
      roles <- getOrganizationRolesClaimSafe(claims)
      rolesObjList = roles.iterator.asScala.toList
      rolesList <- rolesObjList
        .traverse(r => Try(r.asInstanceOf[JsonObject]))
        .as(GenericError("Roles in context are not in json format"))
      roles     <- rolesList.traverse(r => Try(r.get(roleClaim).getAsString()))
    } yield roles

    val roles: Set[String] = maybeRoles match {
      case Failure(e)         =>
        logger.warn(s"Unable to extract userRoles from claims: ${e.getMessage()}")
        Set.empty[String]
      case Success(rolesList) => rolesList.toSet
    }

    getInteropRoleClaimSafe(claims).fold(roles)(roles + _)
  }

  /**
    * Checks if the current request is authorized
    * @param isAuthorized function to check if the request is authorized
    * @param errorMessage message to show if the request is not authorized
    * @param route route to invoke if the request is authorized
    * @param errorMarshaller implicit parameter containing the error marshaller
    * @tparam T error message type
    * @return
    */
  def authorizeInterop[T](isAuthorized: => Boolean, errorMessage: => T)(
    route: Route
  )(implicit errorMarshaller: ToEntityMarshaller[T]): Route = {
    if (isAuthorized) {
      route
    } else {
      logger.error(s"Invalid user role to execute this request")
      complete(StatusCodes.Forbidden, errorMessage)
    }
  }
}
