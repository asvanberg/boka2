package controllers

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import shapeless.contrib.scalacheck._

class JWTSpecification extends Specification with ScalaCheck {
  "Json Web Token" should {
    "reject encoded tokens without a period" in prop { (str: String) => !str.contains(".")  ==> {
        JWT.parse(str) must beNone
      }
    }

    "be able to parse encoded tokens" in {
      val encodedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-IDcSemACt8x4iTMCda8Yhe3iZaWbvV5XKSTbuAn0M"

      JWT.parse(encodedToken) must beSome[JWT]
    }

    "parse claims as json" in {
      val encodedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ImNsYWltcyI.V5GvU8jhwxwzZ9nJ4aymJBVAnzeKPYcsd96uYSci4j4"

      JWT.parse(encodedToken) must beSome.like {
        case JWT(claims) ⇒ claims must beEqualTo(JsString("claims"))
      }
    }

    "validate correctly signed tokens" in {
      val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"

      JWT.isValid(token, "secret".getBytes) must beTrue
    }

    "not validate tokens signed with the wrong secret" in {
      val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"

      JWT.isValid(token, "wrong_secret".getBytes) must beFalse
    }
  }
}
