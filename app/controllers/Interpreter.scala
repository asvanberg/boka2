package controllers

import models.Program
import play.api.mvc._

import scalaz.Alpha.A
import scalaz.std.util.parsing.combinator.parser

trait Interpreter {
  this: Controller ⇒

  def InterpretedAction(block: Request[AnyContent] ⇒ Program[Result]): Action[AnyContent] = InterpretedAction(BodyParsers.parse.default)(block)

  def InterpretedAction[A](parser: BodyParser[A])(block: Request[A] => Program[Result]): Action[A]
}
