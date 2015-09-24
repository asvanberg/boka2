package controllers

import java.sql.Connection
import javax.inject.Inject

import algebra._
import controllers.Application._
import models.{InventoryManagement, LoanManagement}
import play.api.Configuration
import play.api.db.Database
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future
import scalaz._
import scalaz.std.scalaFuture._

class Application @Inject() (val database: Database, val messagesApi: MessagesApi, val client: WSClient, val configuration: Configuration)
  extends Controller with ProductController with Security with Interpreter
  with I18nSupport with InventoryCheckController with PersonController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  override def InterpretedAction[A](parser: BodyParser[A])(block: (Request[A]) ⇒ Program[Result]): Action[A] =
    Action.async(parser) { request ⇒
      interpret(block(request))
    }

  private def interpret[A](program: Program[A]): Future[A] = {
    val interpreter = new (Boka2 ~> Compiled) {
      override def apply[X](fa: Boka2[X]): Compiled[X] =
        fa.run.fold(Interpreters.daisy.apply, _.run.fold(Interpreters.auth.apply, _.run.fold(Interpreters.inventory.apply, Interpreters.loans.apply)))
    }
    val compiled = Free.runFC[Boka2, Compiled, A](program)(interpreter)
    database.withTransaction { conn ⇒
      daisyConfiguration.fold(Future.failed[A](new IllegalStateException("Daisy not configured"))) {
        daisyConfig ⇒
          val c = Application.Configuration(conn, client, daisyConfig)
          compiled.run(c)
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
  type Compiled[A] = ReaderT[Future, Configuration, A]

  final case class Configuration(connection: Connection, client: WSClient, daisy: Daisy.Configuration)
}

object Interpreters {
  object inventory extends (InventoryManagement ~> Compiled) {
    override def apply[A](fa: InventoryManagement[A]): Compiled[A] = DatabaseInterpreter(fa).local[Application.Configuration](_.connection).mapK(Future.successful)
  }
  object loans extends (LoanManagement ~> Compiled) {
    override def apply[A](fa: LoanManagement[A]): Compiled[A] = LoanInterpreter(fa).local[Application.Configuration](_.connection).mapK(Future.successful)
  }
  object auth extends (Auth ~> Compiled) {
    override def apply[A](fa: Auth[A]): Compiled[A] = AuthInterpreter(fa).local[Application.Configuration](_.connection).mapK(Future.successful)
  }
  object daisy extends (Daisy ~> Compiled) {
    override def apply[A](fa: Daisy[A]): Compiled[A] = Daisy.Interpreter.apply(fa).local[Application.Configuration](c ⇒ (c.client, c.daisy))
  }
}