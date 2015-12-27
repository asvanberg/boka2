package models

import java.time.LocalDate

import scala.collection.immutable.List
import scalaz.{Free, Inject}

sealed trait LoanManagement[A]

final case class RecordLoan private[models] (copy: Copy, borrower: PersonId, loanedOn: LocalDate) extends LoanManagement[Ongoing]
final case class CurrentLoan private[models] (copy: Copy) extends LoanManagement[Option[Ongoing]]
final case class ReturnLoan private[models] (ongoing: Ongoing, returned: LocalDate) extends LoanManagement[Returned]
final case class History private[models] (copy: Copy) extends LoanManagement[List[Loan]]

class Loans[F[_]](implicit I: Inject[LoanManagement, F]) {
  type G[A] = Free[F, A]
  
  private def lift[A](a: LoanManagement[A]): G[A] = Free.liftF(I.inj(a))

  def current(copy: Copy): G[Option[Ongoing]] = lift(CurrentLoan(copy))

  def recordLoan(copy: Copy, borrower: PersonId, borrowed: LocalDate): G[Ongoing] =
    lift(RecordLoan(copy, borrower, borrowed))
  
  def returnLoan(ongoing: Ongoing, returned: LocalDate): G[Returned] =
    lift(ReturnLoan(ongoing, returned))
  
  def history(copy: Copy): G[List[Loan]] = lift(History(copy))
}

object Loans {
  implicit def loans[F[_]](implicit I: Inject[LoanManagement, F]): Loans[F] = new Loans
}