package algebra

import anorm._
import java.sql.Connection

import scalaz.{Reader, ~>}

object AuthInterpreter extends (Auth ~> ConnectionIO) {
  override def apply[A](fa: Auth[A]): Reader[Connection, A] = Reader {
    implicit connection ⇒
      fa match {
        case IsAdmin(sub) ⇒
          SQL"""SELECT EXISTS (SELECT 1 FROM admins WHERE subject = $sub) OR NOT EXISTS (SELECT 1 FROM admins)"""
            .as(SqlParser.bool(1).singleOpt)
            .isDefined
      }
  }
}
