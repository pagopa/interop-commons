package it.pagopa.interop.commons.jwt

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Failure, Success, Try}

class UserRolesRetrievalSpec extends AnyWordSpecLike with Matchers {

  "a JWT containing" should {
    "return user roles in a Set if its claims contain user roles" in {

      val s =
        """
          |eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJzdWJqZWN0
          |IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXYuc2VsZmNhcmUucGFnb3BhLml0I
          |iwib3JnYW5pemF0aW9uIjp7ImlkIjoiaW5zdGl0dXRpb25JZCIsInJvbGVzIjpbeyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJwcm9kdWN0Um
          |9sZSJ9LHsicGFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn0seyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJ0ZXN0Um9
          |sZSJ9LHsicGFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn1dLCJncm91cHMiOlsiZ3JvdXBJZCJdLCJmaXNjYWxfY29kZSI6InRh
          |eENvZGUifSwiZGVzaXJlZF9leHAiOjE2NTE4MjQ1NTh9.Z3sFI6K7kiE4O8kLdz0VnSRZhW5S3uHJfkMdPcQ9_N8
          |""".stripMargin

      val jwt                     = SignedJWT.parse(s)
      val claims: JWTClaimsSet    = jwt.getJWTClaimsSet
      val roles: Try[Set[String]] = getUserRoles(claims)

      roles shouldBe Success(Set("productRole", "pippoRole", "testRole"))

    }

    "return user roles as String" in {

      val s =
        """
          |eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJzdWJqZWN0
          |IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXYuc2VsZmNhcmUucGFnb3BhLml0I
          |iwib3JnYW5pemF0aW9uIjp7ImlkIjoiaW5zdGl0dXRpb25JZCIsInJvbGVzIjpbeyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJwcm9kdWN0Um
          |9sZSJ9LHsicGFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn0seyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJ0ZXN0Um9
          |sZSJ9LHsicGFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn1dLCJncm91cHMiOlsiZ3JvdXBJZCJdLCJmaXNjYWxfY29kZSI6InRh
          |eENvZGUifSwiZGVzaXJlZF9leHAiOjE2NTE4MjQ1NTh9.Z3sFI6K7kiE4O8kLdz0VnSRZhW5S3uHJfkMdPcQ9_N8
          |""".stripMargin

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Try[String]   = getUserRolesAsString(claims)

      roles shouldBe Success("productRole,pippoRole,testRole")

    }

    "return error since no roles have been found" in {
      val s =
        """eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJz
          |dWJqZWN0IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXYuc2VsZmNhc
          |mUucGFnb3BhLml0Iiwib3JnYW5pemF0aW9uIjoiMSIsImRlc2lyZWRfZXhwIjoxNjUxODI0NTU4fQ.cpKj6dLXAQdJuegE9aZ11EeJewnZFRU
          |XI6oenRVi7GY""".stripMargin

      val jwt                     = SignedJWT.parse(s)
      val claims: JWTClaimsSet    = jwt.getJWTClaimsSet
      val roles: Try[Set[String]] = getUserRoles(claims)

      roles shouldBe a[Failure[_]]
    }
  }

  "a check on admittable roles" should {

    "return true if no admittable roles are defined" in {
      hasPermissions(Set.empty, List("a", "b", "c")) shouldBe true
    }

    "return false when there is no match between admittable roles and provided ones" in {
      hasPermissions(Set("hello", "there"), List("admin", "operator", "api")) shouldBe false
    }

    "return false when there is a match between admittable roles and provided ones" in {
      hasPermissions(Set("hello", "admin"), List("admin", "operator", "api")) shouldBe true
    }
  }
}
