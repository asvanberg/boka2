package controllers

import controllers.json._
import models.Copy.IdentifierNotUnique
import models.Product.DuplicateName
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html._

import scalaz.std.option._
import scalaz.syntax.all._
import scalaz.{-\/, \/-}

trait ProductController {
  this: Controller with Security with Interpreter with I18nSupport ⇒

  private val productDataForm = Form(mapping(
    "name" → nonEmptyText,
    "description" → optional(text)
  )(ProductData.apply)(ProductData.unapply))

  def addProduct = Authenticated { implicit request ⇒
    Ok(admin.product.add(productDataForm))
  }

  def doAddProduct = InterpretedAction(validation[ProductData]) { implicit request ⇒
    inventory.addProduct(request.body) map {
      case -\/(DuplicateName(_)) =>
        Conflict(error("name" → "A product with that name already exists"))
      case \/-(product) =>
        Ok(Json.toJson(product))
    }
  }

  def listProducts = InterpretedAction { implicit request ⇒
    inventory.listProducts.map(products ⇒ Ok(admin.product.list(products)))
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

  def viewProduct(id: Long) = InterpretedAction { implicit request ⇒
    withProductDetails(id) { details ⇒
      Ok(admin.product.view(details, productDataForm.fill(details.product.data))).pure[Program]
    }
  }

  def doEditProduct(id: Long) = InterpretedAction { implicit request ⇒
    withProductDetails(id) { details ⇒
      productDataForm.bindFromRequest.fold(
        hasErrors ⇒ BadRequest(admin.product.view(details, hasErrors)).pure[Program],
        productData ⇒
          inventory.updateProduct(details.product, productData) map {
            case -\/(DuplicateName(_)) ⇒
              val form = productDataForm.fill(productData)
                .withGlobalError(s"There is already another product named ${productData.name}")
              Conflict(admin.product.view(details, form))
            case \/-(updated) ⇒
              Redirect(routes.Application.viewProduct(updated.id))
                .flashing("success" → "Updated")
          }
      )
    }
  }

  private val copyForm = Form(single("barcode" → nonEmptyText))

  def doAddCopy(id: Long) = InterpretedAction { implicit request ⇒
    withProductDetails(id) { details ⇒
      val feedback = copyForm.bindFromRequest.fold(
        hasErrors ⇒ ("error" → "Invalid barcode").pure[Program],
        barcode ⇒
          inventory.addCopy(details.product, CopyData(barcode, None)) map {
            case -\/(IdentifierNotUnique(_)) ⇒
              "error" → "There is already another copy with that barcode"
            case \/-(copy) ⇒
              "success" → "Copy added"
          }
      )
      feedback map { Redirect(routes.Application.viewProduct(id)).flashing(_) }
    }
  }
}
