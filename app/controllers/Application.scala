package controllers

import java.sql.Connection
import javax.inject.Inject

import play.api.Configuration
import play.api.db.Database
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scalaz._

class Application @Inject() (val database: Database, val messagesApi: MessagesApi, val client: WSClient, val configuration: Configuration)
  extends Controller with ProductController with Security with Interpreter
  with I18nSupport with InventoryCheckController with PersonController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  override def InterpretedAction[A](parser: BodyParser[A])(block: (Request[A]) ⇒ Program[Result]): Action[A] =
    Action(parser) { request ⇒
      interpret(block(request))
    }

  private def interpret[A](program: Program[A]): A = {
    val interpreter = new (Boka2 ~> ({type λ[α] = Reader[Connection, α]})#λ) {
      override def apply[X](fa: Boka2[X]): Reader[Connection, X] =
        fa.run.fold(DatabaseInterpreter.apply, LoanInterpreter.apply)
    }
    val compiled = Free.runFC[Boka2, ({type λ[α] = Reader[Connection, α]})#λ, A](program)(interpreter)
    database.withTransaction(compiled.run)
  }
}