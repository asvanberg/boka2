package se.su.dsv.boka2.api.service

import argonaut.Argonaut._
import models.{Loan, Copy}
import org.http4s.argonaut._
import org.http4s.dsl._
import org.http4s.server.HttpService
import se.su.dsv.boka2.api.model.{ReturnRequest, LoanRequest, CopyDetails}
import se.su.dsv.boka2.api.{loans, inventory, Boka2Op}
import _root_.util.free._

import scalaz.concurrent.Task
import scalaz.std.option._
import scalaz.syntax.bind._
import scalaz.{-\/, \/-, Free, ~>}

object LoanService {
  def apply(interpreter: Boka2Op ~> Task): HttpService = HttpService {
    case GET -> Root / "details" / barcode ⇒
      val prg = for {
        copy ← inventory findCopy barcode
        status ← copy traverseFC Copy.status[Boka2Op]
        product ← copy traverseFC { c ⇒
          inventory.findProduct(c.productId)
        }
        details = ^^(product.flatten, copy, status)(CopyDetails)
      } yield details match {
        case Some(x) ⇒ Ok(x.asJson)
        case None ⇒ NotFound()
      }
      Free.runFC(prg)(interpreter).join
    case request @ POST -> Root / "borrow" ⇒
      jsonValidation.validateAs[LoanRequest](request) {
        loanRequest ⇒
          val prg = for {
            copy ← inventory.findCopy(loanRequest.barcode)
            result ← copy traverseFC {
              Loan.borrow[Boka2Op](_, loanRequest.borrower, loanRequest.borrowed)
            }
          } yield result match {
            case Some(\/-(ongoing)) ⇒
              Ok(ongoing.asJson)
            case Some(-\/(Loan.CopyNotAvailable(ongoing))) ⇒
              Conflict(ongoing.asJson)
            case None ⇒ NotFound()
          }
          Free.runFC(prg)(interpreter).join
      }
    case request @ POST -> Root / "return" ⇒
      jsonValidation.validateAs[ReturnRequest](request) {
        returnRequest ⇒
          val prg = for {
            copy ← inventory findCopy returnRequest.barcode
            ongoing ← copy traverseFC loans.current
            returned ← ongoing traverseFC {
              _ traverseFC { loans.returnLoan(_, returnRequest.returnDate) }
            }
          }
          yield returned match {
            case Some(Some(r)) ⇒ Ok(r.asJson)
            case Some(None) ⇒ Conflict()
            case None => NotFound()
          }
          Free.runFC(prg)(interpreter).join
      }
  }
}
