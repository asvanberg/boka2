package controllers

import play.api.mvc.Security.AuthenticatedBuilder

case class User(name: String, principal: String)

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