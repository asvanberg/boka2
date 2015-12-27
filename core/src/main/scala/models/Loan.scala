package models

import java.time.LocalDate

import util.free._

import scala.collection.immutable._
import scalaz.{Free, \/}
import scalaz.\/.{left, right}

sealed trait Loan {
  def copyId: Identifier
  def borrower: PersonId
  def borrowed: LocalDate
}

final case class Ongoing(copyId: Identifier, borrower: PersonId, borrowed: LocalDate) extends Loan
final case class Returned(copyId: Identifier, borrower: PersonId, borrowed: LocalDate, returned: LocalDate) extends Loan

object Loan {

  final case class CopyNotAvailable(ongoing: Ongoing)

  def borrow[F[_]](
                    copy: Copy,
                    borrower: PersonId,
                    loaned: LocalDate
  )(implicit L: Loans[F]): Free[F, CopyNotAvailable \/ Ongoing] = {
    L.current(copy) flatMap {
      case Some(ongoing) ⇒ pure(left(CopyNotAvailable(ongoing)))
      case None ⇒ L.recordLoan(copy, borrower, loaned) map right
    }
  }

  def returnLoan[F[_]](ongoing: Ongoing, returned: LocalDate)(implicit L: Loans[F]): Free[F, Returned] = {
    L.returnLoan(ongoing, returned)
  }

  def history[F[_]](copy: Copy)(implicit L: Loans[F]): Free[F, List[Loan]] =
    L.history(copy)
}