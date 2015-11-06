package controllers

import controllers.json._
import models.Copy.IdentifierNotUnique
import models.Product.DuplicateName
import models._
import play.api.i18n.I18nSupport
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import util.free._

import scalaz.std.option._
import scalaz.syntax.all._
import scalaz.{-\/, \/-}

trait ProductController {
  this: Controller with Interpreter with I18nSupport with JWTSecurity ⇒

  def doAddProduct = authenticated { jwt ⇒
    InterpretedAction(validation[ProductData]) { implicit request ⇒
      isAdmin(jwt) {
        inventory.addProduct(request.body) map {
          case -\/(DuplicateName(_)) =>
            Conflict(error("name" → "A product with that name already exists"))
          case \/-(product) =>
            Ok(Json.toJson(product))
        }
      }
    }
  }

  def listProducts = authenticated { jwt ⇒
    InterpretedAction { implicit request ⇒
      isAdmin(jwt) {
        inventory.listProducts.map(products ⇒ Ok(Json.toJson(products)))
      }
    }
  }

  private def getProductDetails(id: Long): Program[Option[ProductDetails]] = {
    for {
      maybeProduct ← inventory.findProduct(id)
      maybeCopies ← maybeProduct traverseU inventory.getCopies
    } yield ^(maybeProduct, maybeCopies)(ProductDetails)
  }

  private def withProductDetails(id: Long)(block: ProductDetails ⇒ Program[Result]) =
    for {
      maybeProductDetails ← getProductDetails(id)
      maybeResult ← maybeProductDetails.traverse[Program, Result](block)
    } yield maybeResult getOrElse NotFound

  def viewProduct(id: Long) = authenticated { jwt ⇒
    InterpretedAction { implicit request ⇒
      isAdmin(jwt) {
        withProductDetails(id) { details ⇒
          Ok(Json.toJson(details)).pure[Program]
        }
      }
    }
  }

  def doEditProduct(id: Long) = authenticated { jwt ⇒
    InterpretedAction(validation[ProductData]) { implicit request ⇒
      isAdmin(jwt) {
        for {
          optProduct ← inventory.findProduct(id)
          result ← optProduct traverseFC { oldProduct ⇒
            inventory.updateProduct(oldProduct, request.body) map {
              case -\/(DuplicateName(_)) ⇒
                Conflict(error("name" → "A product with that name already exists"))
              case \/-(editedProduct) ⇒
                Ok(Json.toJson(editedProduct))
            }
          }
        } yield result getOrElse NotFound
      }
    }
  }

  def doAddCopy(id: Long) = authenticated { jwt ⇒
    InterpretedAction(validation[CopyData]) { implicit request ⇒
      for {
        optProduct ← inventory findProduct id
        result ← optProduct traverseFC { product ⇒
          inventory addCopy (product, request.body) map {
            case -\/(IdentifierNotUnique(_)) ⇒
              Conflict(error("barcode" → "There is another copy with the same barcode"))
            case \/-(copy) ⇒
              Ok(Json.toJson(copy))
          }
        }
      } yield result getOrElse NotFound
    }
  }
}
