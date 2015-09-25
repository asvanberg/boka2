package controllers

import algebra._
import models.{InventoryManagement, LoanManagement}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scalaz.{ReaderT, ~>}

object Interpreters {
  type Compiled[A] = ReaderT[Future, Application.Configuration, A]

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