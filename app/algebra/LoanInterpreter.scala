package algebra

import java.sql.Connection
import java.time.LocalDate

import anorm._
import models._

import scalaz._

object LoanInterpreter extends (LoanManagement ~> ConnectionIO) {
  import extraTypes._

  override def apply[A](fa: LoanManagement[A]): Reader[Connection, A] = Reader {
    implicit connection ⇒
      fa match {
        case RecordLoan(copy, borrower, loanedOn) =>
          SQL"""
             INSERT INTO ongoing_loans (identifier, borrower, borrowed)
             VALUES (${copy.code}, $borrower, $loanedOn)
             """.execute()
          Ongoing(copy.code, borrower, loanedOn)
        case CurrentLoan(copy) =>
          SQL"""
             SELECT identifier, borrower, borrowed
             FROM ongoing_loans
             WHERE identifier = ${copy.code}
            """.as(parsers.ongoing.singleOpt)
        case ReturnLoan(Ongoing(identifier, borrower, borrowed), returned) =>
          SQL"""
             INSERT INTO returned_loans (identifier, borrower, borrowed, returned)
             VALUES ($identifier, $borrower, $borrowed, $returned)
            """.execute()
          SQL"""
             DELETE FROM ongoing_loans
             WHERE identifier = $identifier
            """.execute()
          Returned(identifier, borrower, borrowed, returned)
        case History(copy) =>
          SQL"""
             SELECT identifier, borrower, borrowed, returned
             FROM returned_loans
             WHERE identifier = ${copy.code}
            """.as(parsers.returned.*)
      }
  }

  object parsers {
    import anorm.SqlParser._
    import java.time.LocalDateTime

    val ongoing: RowParser[Ongoing] = str("identifier") ~ long("borrower") ~ get[LocalDateTime]("borrowed") map {
      case identifier ~ borrower ~ borrowed ⇒ Ongoing(identifier, PersonId(borrower), borrowed.toLocalDate)
    }

    val returned: RowParser[Returned] = str("identifier") ~ long("borrower") ~ get[LocalDateTime]("borrowed") ~ get[LocalDateTime]("returned") map {
      case identifier ~ borrower ~ borrowed ~ returnDate ⇒ Returned(identifier, PersonId(borrower), borrowed.toLocalDate, returnDate.toLocalDate)
    }
  }

  object extraTypes {
    import anorm.ToStatement
    import java.sql.{Date, PreparedStatement}

    implicit val principalToStatement: ToStatement[PersonId] = new ToStatement[PersonId] {
      override def set(s: PreparedStatement, index: Int, v: PersonId): Unit = s.setLong(index, v.id)
    }

    implicit val localDateToStatement: ToStatement[LocalDate] = new ToStatement[LocalDate] {
      override def set(s: PreparedStatement, index: Int, v: LocalDate): Unit = s.setDate(index, Date.valueOf(v))
    }
  }
}
