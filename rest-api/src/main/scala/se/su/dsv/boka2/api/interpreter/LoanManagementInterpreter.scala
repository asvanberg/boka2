package se.su.dsv.boka2.api.interpreter

import java.time.LocalDate

import doobie.imports._
import models._

import scalaz.syntax.apply._
import scalaz.~>

object LoanManagementInterpreter extends (LoanManagement ~> ConnectionIO) {
  override def apply[A](fa: LoanManagement[A]): ConnectionIO[A] = fa match {
    case RecordLoan(copy, borrower, loanedOn) =>
      queries.insertOngoing(copy.code, borrower, loanedOn)
        .run
        .as(Ongoing(copy.code, borrower, loanedOn))
    case CurrentLoan(copy) =>
      queries.selectOngoing(copy.code)
        .option
    case ReturnLoan(Ongoing(identifier, borrower, borrowed), returned) =>
      val r = queries.insertReturned(identifier, borrower, borrowed, returned)
        .run
        .as(Returned(identifier, borrower, borrowed, returned))
      val d = queries.deleteOngoing(identifier)
        .run
      r <* d
    case History(copy) =>
      queries.selectReturned(copy)
        .list
        .map(identity) // To go from List[Returned] to List[Loan], should history just be List[Returned]?
  }

  object queries {
    def insertOngoing(barcode: Identifier, borrower: PersonId, borrowed: LocalDate): Update0 =
      sql"""INSERT INTO ongoing_loans (identifier, borrower, borrowed)
              VALUES ($barcode, $borrower, $borrowed)"""
        .update

    def selectOngoing(barcode: Identifier): Query0[Ongoing] =
      sql"""SELECT identifier, borrower, borrowed
                  FROM ongoing_loans
                  WHERE identifier = $barcode"""
        .query[Ongoing]

    def insertReturned(identifier: Identifier, borrower: PersonId, borrowed: LocalDate, returned: LocalDate): Update0 =
      sql"""INSERT INTO returned_loans (identifier, borrower, borrowed, returned)
                    VALUES ($identifier, $borrower, $borrowed, $returned)"""
        .update

    def deleteOngoing(identifier: Identifier): Update0 =
      sql"""DELETE FROM ongoing_loans WHERE identifier = $identifier"""
        .update

    def selectReturned(copy: Copy): Query0[Returned] =
      sql"""SELECT identifier, borrower, borrowed, returned
              FROM returned_loans
              WHERE identifier = ${copy.code}"""
        .query[Returned]
  }

  implicit val localDateMeta: Meta[LocalDate] =
    Meta[java.sql.Date].nxmap(_.toLocalDate, java.sql.Date.valueOf)
}
