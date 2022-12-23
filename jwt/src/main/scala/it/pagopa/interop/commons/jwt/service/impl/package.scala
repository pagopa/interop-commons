package it.pagopa.interop.commons.jwt.service

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier

import scala.jdk.CollectionConverters.SetHasAsJava

package object impl {

  final val `at+jwt`: JOSEObjectType = new JOSEObjectType("at+jwt")

  def getClaimsVerifier(
    audience: Set[String] = Set.empty,
    exactMatchClaims: Map[String, String] = Map.empty,
    requiredClaims: Set[String] = Set.empty,
    prohibitedClaims: Set[String] = Set.empty
  ): DefaultJWTClaimsVerifier[SecurityContext] = new DefaultJWTClaimsVerifier[SecurityContext](
    Option(audience).filter(_.nonEmpty).map(_.asJava).orNull,
    Option(exactMatchClaims).filter(_.nonEmpty).map(createJWTClaimsSet).orNull,
    Option(requiredClaims).filter(_.nonEmpty).map(_.asJava).orNull,
    Option(prohibitedClaims).filter(_.nonEmpty).map(_.asJava).orNull
  )

  private def createJWTClaimsSet(claims: Map[String, String]): JWTClaimsSet = {
    val builder: JWTClaimsSet.Builder = new JWTClaimsSet.Builder()
    claims.foreach { case (k, v) => builder.claim(k, v) }
    builder.build()
  }

}
