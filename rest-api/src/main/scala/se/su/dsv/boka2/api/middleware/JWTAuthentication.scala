package se.su.dsv.boka2.api.middleware

import org.http4s.headers.Authorization
import org.http4s.server.{HttpMiddleware, HttpService, Service}
import org.http4s.{OAuth2BearerToken, Response, Status}
import se.su.dsv.boka2.api.util.jwt

import scalaz.concurrent.Task
import scalaz.std.option._
import scalaz.syntax.traverse._

class JWTAuthentication(signer: jwt.Signer)(hasAccess: String => Task[Boolean]) extends HttpMiddleware {

  private val unauthorized = Task.now(Response(Status.Unauthorized))
  private val forbidden = Task.now(Response(Status.Forbidden))

  override def apply(service: HttpService): HttpService =
    Service.lift { request ⇒
      request.headers.get(Authorization)
        .collect { case Authorization(OAuth2BearerToken(token)) => token }
        .traverseU(jwt.parse(signer)(_))
        .map(_.flatMap(_.toOption).flatMap(_.reservedClaims.subject))
        .flatMap(_.traverseU(hasAccess))
        .flatMap(_.fold(unauthorized)(if (_) service(request) else forbidden))
  }
}
