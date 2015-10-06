package controllers

import java.sql.Connection
import javax.inject.Inject

import algebra._
import controllers.Interpreters.Compiled
import models.Auth0Configuration
import play.api.db.Database
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._
import scalaz.std.scalaFuture._

class Application @Inject() (val database: Database, val messagesApi: MessagesApi, val client: WSClient, val auth0Config: Auth0Configuration, val daisyConfig: Daisy.Configuration)
  extends Controller with ProductController with Interpreter
  with I18nSupport with InventoryCheckController with PersonController
  with JWTSecurity {

  def index = Action { implicit request ⇒
    Ok(views.html.admin.index(auth0Config))
  }

  def callback = Action {
    Ok(views.html.admin.callback())
  }

  override def InterpretedAction[A](parser: BodyParser[A])(block: (Request[A]) ⇒ Program[Result]): Action[A] =
    Action(parser) { request ⇒
      interpret(block(request))
    }

  private def interpret(program: Program[Result]): Result = {
    val interpreter = new (Boka2 ~> Compiled) {
      override def apply[X](fa: Boka2[X]): Compiled[X] =
        fa.run.fold(Interpreters.daisy.apply, _.run.fold(Interpreters.auth.apply, _.run.fold(Interpreters.inventory.apply, Interpreters.loans.apply)))
    }
    val compiled = Free.runFC[Boka2, Compiled, Result](program)(interpreter)
    database.withTransaction { conn ⇒
      val c = Application.Configuration(conn, client, daisyConfig)
      // TODO: Fix something better, scalaz.Task?
      Await.result(compiled.run(c), Duration.Inf)
    }
  }

  override def secret: String = auth0Config.secret
}

object Application {
  final case class Configuration(connection: Connection, client: WSClient, daisy: Daisy.Configuration)
}
