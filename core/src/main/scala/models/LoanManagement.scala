package models

import java.time.LocalDate

import scala.collection.immutable.List
import scalaz.{Free, Inject}

sealed trait LoanManagement[A]

private final case class RecordLoan(copy: Copy, borrower: Principal, loanedOn: LocalDate) extends LoanManagement[Ongoing]
private final case class CurrentLoan(copy: Copy) extends LoanManagement[Option[Ongoing]]
private final case class ReturnLoan(ongoing: Ongoing, returned: LocalDate) extends LoanManagement[Returned]
private final case class History(copy: Copy) extends LoanManagement[List[Loan]]

class Loans[F[_]](implicit I: Inject[LoanManagement, F]) {
  type G[A] = Free.FreeC[F, A]
  
  private def lift[A](a: LoanManagement[A]): G[A] = Free.liftFC(I.inj(a))

  def current(copy: Copy): G[Option[Ongoing]] = lift(CurrentLoan(copy))

  def recordLoan(copy: Copy, borrower: Principal, borrowed: LocalDate): G[Ongoing] =
    lift(RecordLoan(copy, borrower, borrowed))
  
  def returnLoan(ongoing: Ongoing, returned: LocalDate): G[Returned] =
    lift(ReturnLoan(ongoing, returned))
  
  def history(copy: Copy): G[List[Loan]] = lift(History(copy))
}

object Loans {
  implicit def loans[F[_]](implicit I: Inject[LoanManagement, F]): Loans[F] = new Loans
}