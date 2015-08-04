package models

import java.time.LocalDate

import util.free._

import scala.collection.immutable._
import scalaz.Free.FreeC
import scalaz.\/
import scalaz.\/.{left, right}

sealed trait Loan {
  def copyId: Long
  def borrower: Principal
  def borrowed: LocalDate
}

final case class Ongoing(copyId: Long, borrower: Principal, borrowed: LocalDate) extends Loan
final case class Returned(copyId: Long, borrower: Principal, borrowed: LocalDate, returned: LocalDate) extends Loan

object Loan {

  final case class CopyNotAvailable(ongoing: Ongoing)

  def borrow[F[_]](
    copy: Copy,
    borrower: Principal,
    loaned: LocalDate
  )(implicit L: Loans[F]): FreeC[F, CopyNotAvailable \/ Ongoing] = {
    L.current(copy) flatMap {
      case Some(ongoing) ⇒ pure(left(CopyNotAvailable(ongoing)))
      case None ⇒ L.recordLoan(copy, borrower, loaned) map right
    }
  }

  def returnLoan[F[_]](ongoing: Ongoing, returned: LocalDate)(implicit L: Loans[F]): FreeC[F, Returned] = {
    L.returnLoan(ongoing, returned)
  }

  def history[F[_]](copy: Copy)(implicit L: Loans[F]): FreeC[F, List[Loan]] =
    L.history(copy)
}