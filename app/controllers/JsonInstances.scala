package controllers

import controllers.ValidationReads._
import models.{Product, ProductData}
import play.api.libs.json.{Writes, Reads, Json}
import scalaz.std.function._
import scalaz.syntax.monad._

trait JsonInstances extends JsonWrites with JsonReads

trait JsonWrites {
  implicit val productDataReads: Writes[ProductData] = Json.writes[ProductData]
  implicit val productWrites: Writes[Product] = Json.writes[Product]
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
