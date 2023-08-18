package it.pagopa.interop.commons.jwt

import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.commons.jwt.getExternalId

class ExternalIdRetrievalSpec extends AnyWordSpecLike with Matchers with JWTMockHelper {

  "a JWT containing" should {
    "return origin and value" in {

      val s =
        """
          |eyJraWQiOiJcIi1NUkpqSll2ZS1QdElHS3IwYnJaeEEtM2FmRE01NU5pUXo3OHdTeWI4ZFVcIiIsImFsZyI6IlJTMjU2In0.eyJvcmdhbml6YXRpb25JZCI6IjM3ZGU3MTcwLTYxMGMtN
          |DEzZi04NTM2LTBkZmUxNGQ2OGJlZCIsInN1YiI6ImRkNTFiYzVkLTcxNmEtNDU1ZS04MzkzLTA0NTIwZWM1YWQ1OCIsImF1ZCI6InRlc3QiLCJuYmYiOjE2OTE2NzczNTgsImlzcyI6I
          |mQ2ODhlOThlLTRkODMtNGFhNy1hNzg0LTI1ODU5ZGM4M2M2NyIsImV4dGVybmFsSWQiOnsib3JpZ2luIjoiSVBBIiwidmFsdWUiOiIxMjM0NSJ9LCJ1c2VyLXJvbGVzIjoiYWRtaW4sI
          |G0ybSIsImV4cCI6NDEwMjQ0NDc5OSwic2VsZmNhcmVJZCI6IjU5MTMyNDdhLWYzZjQtNDg3MS1iYWZhLTJkNTYwYTQ5OGJhMCIsImlhdCI6MTY5MTY3NzM1OCwianRpIjoiZDNiODFkM
          |jctYWJhMy00NGE2LWIxMTktMjE2ZTgwOTZhZDYyIn0.DZCn29LvjbKjhzZBmO2e2fKyhwUdbHcKMj1O6TeyDosf7TgoMy69NwZpuuLuWAOtonpVbIQE5zbBF2tBk4Wtdf_x91o0IQ7QA
          |Q9keILSPXOZ9OBqeTvXLAFbCiIYOWHOMHOmeVH43oggeFW3Ja_C0_6bM9DUUoNBMKAf-AnpueVaL7Sh58tytSvzli9i-nge6w_G-wyIGkg3XIi-FfcwsMswLWNEnRdJW2ajk8sdtjqXk
          |ccCIz-_kq2W-8bu4p7XjK3YqdVD0D1Fk8QhpBWsyRfuMErK82pUW34ukQGUkphkL6DMKbVg4xdr-UQDI9ClxY7NnGdK4I_NhWMKaU4CLQ
          |""".stripMargin

      val parsedJwt                            = SignedJWT.parse(s)
      val claims: JWTClaimsSet                 = parsedJwt.getJWTClaimsSet
      val externalId: Option[(String, String)] = getExternalId(claims)
      externalId shouldBe Some(("IPA", "12345"))
    }
    "return None if only origin" in {

      val s =
        """
          |eyJraWQiOiJcIm9FWFgzNjdPTFpfWXpyY3UwWDdWek1KVmQwSnUtNXM3Y3VYRWdwNFFQZlFcIiIsImFsZyI6IlJTMjU2In0.eyJvcmdhbml6YXRpb25JZCI6ImUxOGQ3MjVjLTdiNjAtND
          |ZjNS1hNDMyLTVkYmE1YTJmNDlkNCIsInN1YiI6IjE4MTQyNWQxLTRiYTMtNGEwMS1iNDQ1LWE2YzJkZTNhYjc5ZSIsImF1ZCI6InRlc3QiLCJuYmYiOjE2OTE2Nzc1ODUsImlzcyI6IjQ2
          |NzMyNzY5LWFhNWUtNDYxYi1hZDEwLWM0NDcyZTllOTRkOCIsImV4dGVybmFsSWQiOnsib3JpZ2luIjoiSVBBIn0sInVzZXItcm9sZXMiOiJhZG1pbiwgbTJtIiwiZXhwIjo0MTAyNDQ0Nz
          |k5LCJzZWxmY2FyZUlkIjoiY2NhNjcxMDgtOTU4MS00YWYwLWJhMGMtNTA1NzEyODhlNzZlIiwiaWF0IjoxNjkxNjc3NTg1LCJqdGkiOiI4ZmIzMGVjYi00NjVkLTRmZTEtYTEzNi0wNzE2
          |YTU0ZDg5ZDUifQ.NXxtyAJFl2kXon5Nt0Ak1kF6SDkOBm1YSTfjdnh1bHea_hFmXqFWU0TYWg9Iowk-KZblVldwLmaE95pDIuBzgwN_lrVXhfe2opEqIm7ynjoGh_XuYg8TA438FtGArfb
          |f_FSISRHBYWtWfc_xz7UtpaHlFzKq6oWdDh0Qa-H8QCg9WsOGd4QOIQ3jcccpELiOGTS-UZCFv5Shj5qv_ty1mFkslHVckt28f1oSSByt-0bYoZkMB0w3qTwAoI0ZATLCQSj-USkqtbotn
          |iwVOSSCIcHc-kknpE7svasFPRRCKOSLMVq5fFnnzr6Iwr4KQ0vRTZcd2XaBkR7NGfVM2Ngzjw
          |""".stripMargin

      val parsedJwt                            = SignedJWT.parse(s)
      val claims: JWTClaimsSet                 = parsedJwt.getJWTClaimsSet
      val externalId: Option[(String, String)] = getExternalId(claims)

      externalId shouldBe None
    }
    "return None if only value" in {

      val s =
        """
          |eyJraWQiOiJcImFVUUJNWXhFb0o5ejhOOFNSal9JeVk3OHNlRzd4OVIwNUdjNHNFN1htUmNcIiIsImFsZyI6IlJTMjU2In0.eyJvcmdhbml6YXRpb25JZCI6ImRlMDE5N2JmLTgwYmMtNDM
          |0NC1iY2QzLTc5YThmNDlkNDg1ZiIsInN1YiI6ImIzOTlmNzg2LTkxMzctNDI2Mi1iNGM4LWMxMzc5Njg4YzliNSIsImF1ZCI6InRlc3QiLCJuYmYiOjE2OTE2Nzc3NTAsImlzcyI6Ijk0YW
          |NhZTk2LTllMDMtNDQ4Mi05YzNlLTZlNDU5NDcxYzBhZSIsImV4dGVybmFsSWQiOnsidmFsdWUiOiIxMjM0NSJ9LCJ1c2VyLXJvbGVzIjoiYWRtaW4sIG0ybSIsImV4cCI6NDEwMjQ0NDc5O
          |Swic2VsZmNhcmVJZCI6IjUzZmMwZTAwLWYwOTgtNGMwYS1iMGQ3LTVhYTk4YjQ0MzdmMiIsImlhdCI6MTY5MTY3Nzc1MCwianRpIjoiNTRkZGJiMjEtZTUzOS00NzZjLWJlNzctMGY0ZDg1
          |ZTBmOTAwIn0.X6yQ2Vtc5tw8sciqzkJGbwelqU2geZ3J_LAcFhB-vG_-f6u3cSCOlaoMF28lzpkzxq1f9XDp0_oKhOzlDxv9S4PkR7MVvH1a0KrJ2zFxqOt3XRZbwUMc6h6pbWrDupORTjO
          |KrGAtXx1U0EnngypiS4YTXvExIpnNKuDj4EnY0xCCBpYOAZ9GwgyT9utVcEWZ5Rw3zPTKOBb9epM0dIAZRujbG2GR-u3zV3rl430K48ISLg0m_SK6yu9k0qZVrcU6VwKynhazQR6rw7Hlen
          |MMJ7orfl4kVcXISo11TMKotX_stSuQzne-2wsM5A_0BqNxsj-4UlbzYeSWBacTHW8QvQ
          |""".stripMargin

      val parsedJwt                            = SignedJWT.parse(s)
      val claims: JWTClaimsSet                 = parsedJwt.getJWTClaimsSet
      val externalId: Option[(String, String)] = getExternalId(claims)

      externalId shouldBe None
    }
    "return None if no values" in {

      val s =
        """
          |eyJraWQiOiJcImQxUTlQd3JETko1RC1FZmtMQzZkMUNqYlRwX1dtOXo3SjQ0eDRFYWJCTzRcIiIsImFsZyI6IlJTMjU2In0.eyJvcmdhbml6YXRpb25JZCI6IjBmMzVlZGU2LTU2NTMtNGY0
          |Ni04YWQ0LTFkMDhmMTdkNTU3MyIsInN1YiI6ImJhYjIxZjJkLWU0OTAtNDk0My04Nzk5LTBlMTAyMTYyZjM0MyIsImF1ZCI6InRlc3QiLCJuYmYiOjE2OTE2Nzc4NzcsImlzcyI6IjRiMDdh
          |NWVmLTI3MWYtNDc3YS05MmEwLTAyZDBmMDE2ZjMzYiIsInVzZXItcm9sZXMiOiJhZG1pbiwgbTJtIiwiZXhwIjo0MTAyNDQ0Nzk5LCJzZWxmY2FyZUlkIjoiMDM0YmIzY2ItYTc0Yi00OWFm
          |LTkxMDAtZDQxNjkwYzE5M2NjIiwiaWF0IjoxNjkxNjc3ODc3LCJqdGkiOiI0OTVjNDUzYS02YWVlLTQzZmUtYmJiZi01NWEzOTk2MTBjNTAifQ.T6wHL6qta4rE2DJiVz4mBUPicgqr1SQxj
          |srUuy94hE7NF8ztQ_LJMakjybV2yxhqUgo-idh7St0AzBVjaTkf7rdJJ5cfukWRMN88MQODJNAr3YRrjbbUVy3d6YNvrmsRr-7jB3Bxxj1ghmXBdZ59C434LO4D0tB0kaXodX2jI95VGLIKK
          |0MjV0aUCiICqIjbEJwDDTMPK21YyIhNrdYW66M6zHFaBLTf2g9mKE-NE4GM9IFhUv26g7wFzA0mh8v-nFe33UuNCXzgK6pt8X7QgOEjFfK4rqHhrk13zsHPB2yWY66vTqAaclMY_mBVw8cpO
          |vebyC59ncWrMDgIjjzLJg
          |""".stripMargin

      val parsedJwt                            = SignedJWT.parse(s)
      val claims: JWTClaimsSet                 = parsedJwt.getJWTClaimsSet
      val externalId: Option[(String, String)] = getExternalId(claims)

      externalId shouldBe None
    }
  }
}
