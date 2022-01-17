package it.pagopa.pdnd.interop.commons.jwt.service

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier

import scala.jdk.CollectionConverters.SetHasAsJava

package object impl {

  def getClaimsVerifier(
    audiences: Set[String] = Set.empty,
    exactMatchClaims: Option[JWTClaimsSet] = None,
    requiredClaims: Set[String] = Set.empty,
    prohibitedClaims: Set[String] = Set.empty
  ): DefaultJWTClaimsVerifier[SecurityContext] = {
    new DefaultJWTClaimsVerifier[SecurityContext](
      Option(audiences).filter(_.nonEmpty).map(_.asJava).orNull,
      exactMatchClaims.orNull,
      Option(requiredClaims).filter(_.nonEmpty).map(_.asJava).orNull,
      Option(prohibitedClaims).filter(_.nonEmpty).map(_.asJava).orNull
    )
  }
}
