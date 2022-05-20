package it.pagopa.interop.commons

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.shaded.json.{JSONArray, JSONObject}
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.commons.utils.USER_ROLES
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

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

  final val M2M_ROLES = Map("role" -> M2M_ROLE)

  private[jwt] def rsaVerifier(jwkKey: String): Try[RSASSAVerifier] = {
    Try {
      val jwk: JWK  = JWK.parse(jwkKey)
      val publicKey = jwk.toRSAKey
      new RSASSAVerifier(publicKey)
    }
  }

  private[jwt] def ecVerifier(jwkKey: String): Try[ECDSAVerifier] =
    Try {
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
        val requestRoles = contexts.toMap.get(USER_ROLES).map(_.split(",")).getOrElse(Array.empty)
        x.intersect(requestRoles).size > 0
    }

  private[jwt] def getUserRoles(claims: JWTClaimsSet): Set[String] = {
    val roleSetOpt = Try {
      val roles: Iterator[AnyRef] =
        claims.getJSONObjectClaim(organizationClaim).get("roles").asInstanceOf[JSONArray].iterator().asScala
      roles.foldLeft(Set.empty[Option[String]])((set, item) =>
        set + Option(item.asInstanceOf[JSONObject].getAsString(roleClaim))
      )
    }.getOrElse(Set.empty)

    val interopRoleClaim = Option(claims.getStringClaim(roleClaim))
    (roleSetOpt + interopRoleClaim).flatten

  }

  def getUserRolesAsString(claims: JWTClaimsSet): Try[String] = Try {
    getUserRoles(claims).mkString(",")
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
