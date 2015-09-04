package controllers

import models.{Copy, InventoryCheckCopy}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Controller
import util.free._

import scalaz.std.list._
import scalaz.syntax.applicative._

trait InventoryCheckController {
  this: Controller with Interpreter with I18nSupport ⇒

  def viewInventoryCheck = InterpretedAction { implicit request ⇒
    Ok(views.html.admin.inventoryCheck()).pure[Program]
  }

  def icjson = InterpretedAction { implicit request ⇒
    for {
      products ← inventory.listProducts
      result ← products.traverseFC { product ⇒
        for {
          copies ← inventory.getCopies(product)
          result ← copies.traverseFC { copy ⇒
            for {
              status ← Copy.status[Boka2](copy)
            } yield InventoryCheckCopy(product, copy, status)
          }
        } yield result
      }
    } yield Ok(Json.toJson(result.flatten))
  }
}
