package it.pagopa.interop.commons.jwt

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserRolesRetrievalSpec extends AnyWordSpecLike with Matchers {

  "a JWT containing" should {

    "return user roles in a Set if its claims contain M2M role" in {

      val s =
        """
          |eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWI
          |iOiJzdWJqZWN0IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXY
          |uc2VsZmNhcmUucGFnb3BhLml0Iiwicm9sZSI6Im0ybSJ9.jJGYELWDY0loQHZLwzQwo-VERFGlHkqhCQq9WZd1DhE
          |""".stripMargin

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set("m2m")
    }

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

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set("productRole", "pippoRole", "testRole")
    }

    "return user roles in a Set if its claims contain user roles, with M2M role also" ignore {

      val s =
        """
          |eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJzdWJq
          |ZWN0IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXYuc2VsZmNhcmUucGFnb3
          |BhLml0Iiwicm9sZSI6Im0ybSIsIm9yZ2FuaXphdGlvbiI6eyJpZCI6Imluc3RpdHV0aW9uSWQiLCJyb2xlcyI6W3sicGFydHlSb2xlIjoiT1BFUkFU
          |T1IiLCJyb2xlIjoicHJvZHVjdFJvbGUifSx7InBhcnR5Um9sZSI6Ik9QRVJBVE9SIiwicm9sZSI6InBpcHBvUm9sZSJ9LHsicGFydHlSb2xlIjoiT1
          |BFUkFUT1IiLCJyb2xlIjoidGVzdFJvbGUifSx7InBhcnR5Um9sZSI6Ik9QRVJBVE9SIiwicm9sZSI6InBpcHBvUm9sZSJ9XSwiZ3JvdXBzIjpbImdy
          |b3VwSWQiXSwiZmlzY2FsX2NvZGUiOiJ0YXhDb2RlIn0sImRlc2lyZWRfZXhwIjoxNjUxODI0NTU4fQ._rMe7eYoFLpoWpxOpus6q7TbLnse17ipmXf
          |NUz6DefM
          |""".stripMargin

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set("productRole", "pippoRole", "testRole", "m2m")
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
      val roles: String        = getUserRoles(claims).mkString(",")

      roles shouldBe "productRole,pippoRole,testRole"

    }

    "return the interop user-roles before the selfcare roles if already present in the jwt" in {
      val s =
        """|eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzd
           |WIiOiJzdWJqZWN0IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9
           |kZXYuc2VsZmNhcmUucGFnb3BhLml0IiwidXNlci1yb2xlcyI6InBhcGVyaW5vIiwib3JnYW5pemF0aW9uIjp7ImlkIjoiaW5zdGl0d
           |XRpb25JZCIsInJvbGVzIjpbeyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJwcm9kdWN0Um9sZSJ9LHsicGFydHlSb2xlIjo
           |iT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn0seyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJ0ZXN0Um9sZSJ9LHsic
           |GFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn1dLCJncm91cHMiOlsiZ3JvdXBJZCJdLCJmaXNjYWxfY29kZSI
           |6InRheENvZGUifSwiZGVzaXJlZF9leHAiOjE2NTE4MjQ1NTh9.E_LHsdRJnRyns6_PzTQCDsZTuusWCeFrt2ogk_pMyWs""".stripMargin

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set("paperino")
    }

    "return the interop role before the interop user-roles or selfcare roles if already present in the jwt" in {
      val s                    =
        """|eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTR
           |kZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJzdWJqZWN0IiwiaWF0IjoxNjUxODI
           |0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9
           |kZXYuc2VsZmNhcmUucGFnb3BhLml0Iiwicm9sZSI6InBpcHBvIiwidXNlci1yb2xlcyI
           |6InBhcGVyaW5vIiwib3JnYW5pemF0aW9uIjp7ImlkIjoiaW5zdGl0dXRpb25JZCIsInJ
           |vbGVzIjpbeyJwYXJ0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJwcm9kdWN0Um9sZSJ
           |9LHsicGFydHlSb2xlIjoiT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn0seyJwYXJ
           |0eVJvbGUiOiJPUEVSQVRPUiIsInJvbGUiOiJ0ZXN0Um9sZSJ9LHsicGFydHlSb2xlIjo
           |iT1BFUkFUT1IiLCJyb2xlIjoicGlwcG9Sb2xlIn1dLCJncm91cHMiOlsiZ3JvdXBJZCJ
           |dLCJmaXNjYWxfY29kZSI6InRheENvZGUifSwiZGVzaXJlZF9leHAiOjE2NTE4MjQ1NTh
           |9.SIgmiUuJeGvY98Ndc7f2bjFTXygRxAZyL_zuFgrSnxA""".stripMargin
      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set("pippo")
    }

    "return error since no roles have been found" in {
      val s =
        """eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJlZmM3Y2IzZC1jNzkxLTRkZDMtYjM3MC1mZTUzYjU1MjA2ZGMiLCJzdWIiOiJz
          |dWJqZWN0IiwiaWF0IjoxNjUxODI0NTU2LCJleHAiOjE2NTE4MjQ1NjEsImF1ZCI6InJlYWxtIiwiaXNzIjoiaHR0cHM6Ly9kZXYuc2VsZmNhc
          |mUucGFnb3BhLml0Iiwib3JnYW5pemF0aW9uIjoiMSIsImRlc2lyZWRfZXhwIjoxNjUxODI0NTU4fQ.cpKj6dLXAQdJuegE9aZ11EeJewnZFRU
          |XI6oenRVi7GY""".stripMargin

      val jwt                  = SignedJWT.parse(s)
      val claims: JWTClaimsSet = jwt.getJWTClaimsSet
      val roles: Set[String]   = getUserRoles(claims)

      roles shouldBe Set.empty
    }
  }

  "a check on admittable roles" should {

    "return false if no admittable roles are defined" in {
      hasPermissions()(Seq(USER_ROLES -> "a,b,c")) shouldBe false
    }

    "return false when there is no match between admittable roles and provided ones" in {
      hasPermissions("hello", "there")(Seq(USER_ROLES -> "admin,operator,api")) shouldBe false
    }

    "return true when there is a match between admittable roles and provided ones" in {
      hasPermissions("hello", "admin")(Seq(USER_ROLES -> "admin,operator,api")) shouldBe true
    }
  }
}
