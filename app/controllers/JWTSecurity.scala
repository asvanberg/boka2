package controllers

import algebra.{Auth, IsAdmin}
import play.api.libs.json.{JsDefined, JsString}
import play.api.mvc.Security.Authenticated
import play.api.mvc.{Controller, EssentialAction, Result}
import util.free.pure

import scalaz.{Free, Inject}

trait JWTSecurity {
  this: Controller ⇒
  
  def secret: String
  
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

  def isAdmin[F[_]](jwt: JWT)(block: ⇒ Free[F, Result])(implicit I: Inject[Auth, F]): Free[F, Result] =
    jwt.claims \ "sub" match {
      case JsDefined(JsString(value)) ⇒
        for {
          isAdmin ← Free.liftF(I.inj(IsAdmin(value)))
          result ← if (isAdmin) block else pure[F](Forbidden)
        } yield result
      case _ ⇒ pure[F](Unauthorized)
    }
}

object JWTSecurity {
  private val Bearer = "Bearer (.*)".r
}