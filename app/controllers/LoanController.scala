package controllers

import controllers.json._
import models._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Controller
import util.free._

import scalaz.std.option._
import scalaz.syntax.apply._
import scalaz.{-\/, \/-}

trait LoanController {
  this: Controller with Interpreter with I18nSupport with JWTSecurity ⇒

  def copyDetails(barcode: Identifier) = authenticated { jwt ⇒
    InterpretedAction { implicit request ⇒
      isAdmin(jwt) {
        for {
          copy ← inventory findCopy barcode
          status ← copy traverseFC Copy.status[Boka2]
          product ← copy traverseFC { c ⇒
            inventory.findProduct(c.productId)
          }
          details = ^^(product.flatten, copy, status)(CopyDetails)
        } yield details match {
          case Some(x) => Ok(Json.toJson(x))
          case None => NotFound
        }
      }
    }
  }

  def recordLoan = authenticated { jwt ⇒
    InterpretedAction(parse.json[LoanRequest]) { implicit request ⇒
      isAdmin(jwt) {
        for {
          copy ← inventory.findCopy(request.body.barcode)
          result ← copy traverseFC {
            Loan.borrow[Boka2](_, request.body.borrower, request.body.borrowed)
          }
        } yield result match {
          case Some(\/-(ongoing)) ⇒ Ok(Json.toJson(ongoing))
          case Some(-\/(Loan.CopyNotAvailable(ongoing))) ⇒ Conflict(Json.toJson(ongoing))
          case None ⇒ NotFound
        }
      }
    }
  }

  def returnCopy = authenticated { jwt ⇒
    InterpretedAction(parse.json[ReturnRequest]) { implicit request ⇒
      isAdmin(jwt) {
        for {
          copy ← inventory findCopy request.body.barcode
          ongoing ← copy traverseFC loans.current
          returned ← ongoing traverseFC {
            _ traverseFC { loans.returnLoan(_, request.body.returnDate)
            }
          }
        }
        yield returned match {
          case Some(Some(r)) ⇒ Ok(Json.toJson(r))
          case Some(None) ⇒ Conflict
          case None => NotFound
        }
      }
    }
  }
}
