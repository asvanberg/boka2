package models

import models.Copy.{Available, Borrowed}
import play.api.libs.json.{JsString, JsObject, JsValue, Writes}

final case class InventoryCheckCopy(product: Product, copy: Copy, status: Copy.Status)

object InventoryCheckCopy extends ((Product, Copy, Copy.Status) ⇒ InventoryCheckCopy) {
  implicit val writes = new Writes[InventoryCheckCopy] {
    override def writes(o: InventoryCheckCopy): JsValue =
      JsObject(
        Map[String, JsValue](
          "name" → JsString(o.product.name),
          "code" → JsString(o.copy.code),
          "status" → JsString(o.status match {
            case Borrowed(_) ⇒ "loaned"
            case Available ⇒ "unknown"
          })
        )
      )
  }
}