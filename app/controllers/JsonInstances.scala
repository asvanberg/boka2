package controllers

import controllers.ValidationReads._
import models.{Product, ProductData}
import play.api.libs.json._
import scalaz.std.function._
import scalaz.syntax.monad._

trait JsonInstances extends JsonWrites with JsonReads

trait JsonWrites {
  implicit val productWrites: Writes[Product] = Writes { case Product(id, ProductData(name, description)) ⇒
    Json.obj(
      "id" → JsNumber(id),
      "name" → JsString(name),
      "description" → description.fold[JsValue](JsNull)(JsString)
    )
  }
}

trait JsonReads {
  // All strings we want should be non-empty, use Option[String] otherwise
  implicit val nonEmptyStringRead: Reads[String] = Reads.StringReads.filter(_.trim.nonEmpty)

  implicit val productDataVReads: ValidationParser[ProductData] = for {
    name ← read[String]("name", "Name is required")
    description ← read[Option[String]]("description", "Invalid description")
  } yield {
      (name |@| description)(ProductData)
    }
}
