package controllers

import controllers.ValidationReads._
import models.Copy.{Borrowed, Available, Status}
import models._
import play.api.libs.json._
import scalaz.NonEmptyList
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

  implicit val copyWrites: Writes[Copy] = Writes { case Copy(_, CopyData(barcode, note)) ⇒
    Json.obj(
      "barcode" → JsString(barcode),
      "note" → note.fold[JsValue](JsNull)(JsString)
    )
  }

  implicit val productDetailsWrites: Writes[ProductDetails] = Json.writes[ProductDetails]

  implicit val copyStatusWrites: Writes[Copy.Status] = new Writes[Status] {
    override def writes(o: Status): JsValue = o match {
      case Available => JsString("available")
      case Borrowed(current) => JsString("borrowed")
    }
  }

  implicit val copyDetailsWrites: Writes[CopyDetails] = Json.writes[CopyDetails]

  implicit val personId: Writes[PersonId] = Writes { x ⇒ Writes.LongWrites.writes(x.id) }

  implicit val ongoingWrites: Writes[Ongoing] = Json.writes[Ongoing]
}

trait JsonReads {
  implicit val nonEmptyStringRead: Reads[String] = Reads.StringReads.map(_.trim)

  implicit val productDataVReads: ValidationParser[ProductData] = for {
    name ← read[String]("name", "Name is required") andThen {
      _.ensure(NonEmptyList("name" → "Name must not be empty"))(_.nonEmpty)
    }
    description ← read[Option[String]]("description", "Invalid description")
  } yield {
    (name |@| description)(ProductData)
  }

  implicit val copyDataVReads: ValidationParser[CopyData] = for {
    name ← read[String]("barcode", "Barcode is required") andThen {
      _.ensure(NonEmptyList("barcode" → "Barcode must not be empty"))(_.nonEmpty)
    }
    description ← read[Option[String]]("note", "Invalid note")
  } yield {
    (name |@| description)(CopyData)
  }

  implicit val personIdReads: Reads[PersonId] = Reads.LongReads map PersonId

  implicit val loanRequestReads: Reads[LoanRequest] = Json.reads[LoanRequest]
}
