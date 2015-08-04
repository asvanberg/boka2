package controllers

import java.sql.Connection
import javax.inject.Inject

import play.api._
import play.api.db.Database
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc._

import scalaz._

class Application @Inject() (val database: Database, val messagesApi: MessagesApi)
  extends Controller with ProductController with Security with Interpreter with I18nSupport {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  override def InterpretedAction[A](parser: BodyParser[A])(block: (Request[A]) ⇒ Program[Result]): Action[A] =
    Action(parser) { request ⇒
      interpret(block(request))
    }

  private def interpret[A](program: Program[A]): A = {
    val compiled = Free.runFC[Boka2, ({type λ[α] = Reader[Connection, α]})#λ, A](program)(DatabaseInterpreter)
    database.withTransaction(compiled.run)
  }
}