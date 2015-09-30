package controllers

import java.sql.Connection
import javax.inject.Inject

import algebra._
import controllers.Interpreters.Compiled
import play.api.Configuration
import play.api.db.Database
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scalaz._
import scalaz.std.scalaFuture._
import scalaz.std.option._

class Application @Inject() (val database: Database, val messagesApi: MessagesApi, val client: WSClient, val configuration: Configuration)
  extends Controller with ProductController with Security with Interpreter
  with I18nSupport with InventoryCheckController with PersonController {

  def index = Action { implicit request ⇒
    Ok(views.html.admin.index())
  }

  def login = Action { implicit request ⇒
    def cfgVar(k: String) = configuration.getString(k)
    import scalaz.syntax.apply._
    val auth0Config = ^^(cfgVar("AUTH0_CALLBACK_URL"), cfgVar("AUTH0_CLIENT_ID"), cfgVar("AUTH0_DOMAIN"))(Auth0Configuration)
    auth0Config.fold[Result](ServiceUnavailable) { config ⇒
      Ok(views.html.admin.login(config))
    }
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
      daisyConfiguration.fold[Result](ServiceUnavailable("Daisy not configured")) {
        daisyConfig ⇒
          val c = Application.Configuration(conn, client, daisyConfig)
          // TODO: Fix something better, scalaz.Task?
          Await.result(compiled.run(c), Duration.Inf)
      }
    }
  }

  lazy val daisyConfiguration: Option[Daisy.Configuration] = {
    for {
      host ← configuration.getString("daisy.api.host")
      port ← configuration.getInt("daisy.api.port")
      username ← configuration.getString("daisy.api.username")
      password ← configuration.getString("daisy.api.password")
    } yield {
      Daisy.Configuration(host, port, username, password)
    }
  }
}

object Application {
  final case class Configuration(connection: Connection, client: WSClient, daisy: Daisy.Configuration)
}
