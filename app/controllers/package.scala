import algebra.{Daisy0, Daisy, Auth}
import controllers.ValidationReads.ValidationParser
import models.{Loans, LoanManagement, Inventory, InventoryManagement}
import play.api.libs.json.{JsValue, JsString, JsObject, Json}
import play.api.mvc.{Results, BodyParsers, BodyParser}

import scala.concurrent.ExecutionContext
import scalaz.{Failure, Success, Coproduct}
import scalaz.Free.FreeC

package object controllers {
  type F0[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type F1[A] = Coproduct[Auth, F0, A]
  type Boka2[A] = Coproduct[Daisy, F1, A]
  type Program[A] = FreeC[Boka2, A]

  object inventory extends Inventory[Boka2]

  object loans extends Loans[Boka2]

  object daisy extends Daisy0[Boka2]

  object json extends JsonInstances {
    def error(fieldError: (String, String), more: (String, String)*): JsValue = {
      val errors = (fieldError +: more) map { case (field, message) ⇒ field → JsString(message) }
      JsObject(Map("fields" → JsObject(errors)))
    }

    def validation[A](implicit R: ValidationParser[A], ec: ExecutionContext) = BodyParsers.parse.json.validate {
      json ⇒
        R.apply(json) match {
          case Success(a) => Right(a)
          case Failure(errors) =>
            Left(Results.BadRequest(error(errors.head, errors.tail: _*)))
        }
    }
  }
}
