package controllers

import algebra.{Auth, IsAdmin}
import play.api.libs.json.{JsDefined, JsString}
import play.api.mvc.Security.{Authenticated, AuthenticatedBuilder}
import play.api.mvc.{Controller, EssentialAction, Result}
import util.free.pure

import scalaz.Free.FreeC
import scalaz.{Free, Inject}

final case class User(name: String, principal: String)

trait Security {
  import Security._

  object Authenticated extends AuthenticatedBuilder(rh ⇒
    for {
      principal ← rh.headers.get(principalHeader)
      name ← rh.headers.get(nameHeader)
    } yield User(name, principal)
  )

}

object Security {
  val principalHeader = "eduPersonPrincipalName"
  val nameHeader = "cn"
}

trait JWTSecurity {
  this: Controller ⇒
  
  def secret: Array[Byte]
  
  def authenticated(block: JWT ⇒ EssentialAction): EssentialAction = {
    Authenticated[JWT](
      userinfo = request ⇒ {
        for {
          header ← request.headers.get(AUTHORIZATION)
          tokenString ← header match {
            case JWTSecurity.Bearer(tokenString) ⇒ Some(tokenString)
            case _ ⇒ None
          }
          if JWT.isValid(tokenString, secret)
          token ← JWT.parse(tokenString)
        } yield token
      },
      onUnauthorized = _ ⇒ Unauthorized
    )(block)
  }

  def isAdmin[F[_]](jwt: JWT)(block: ⇒ FreeC[F, Result])(implicit I: Inject[Auth, F]): FreeC[F, Result] =
    jwt.claims \ "sub" match {
      case JsDefined(JsString(value)) ⇒
        for {
          isAdmin ← Free.liftFC(I.inj(IsAdmin(value)))
          result ← if (isAdmin) block else pure[F](Forbidden)
        } yield result
      case _ ⇒ pure[F](Unauthorized)
    }
}

object JWTSecurity {
  private val Bearer = "Bearer (.*)".r
}