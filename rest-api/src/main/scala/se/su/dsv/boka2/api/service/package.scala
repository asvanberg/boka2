package se.su.dsv.boka2.api

import java.nio.charset.StandardCharsets
import java.time.{ZoneId, Clock, Instant, LocalDate}

import argonaut.Argonaut._
import argonaut._
import models._
import models.Copy.{Available, Borrowed}
import org.http4s.{Response, Request, EntityDecoder}
import org.http4s.argonaut._
import org.http4s.dsl._
import se.su.dsv.boka2.api.model._

import scalaz.concurrent.Task
import scalaz.{NonEmptyList, ValidationNel}
import scalaz.syntax.apply._
import scalaz.syntax.std.option._

package object service {
  implicit val productEncodeJson: EncodeJson[Product] = EncodeJson[Product] {
    case Product(id, ProductData(name, description)) ⇒
      ("id" := id) ->: ("name" := name) ->: ("description" := description) ->: jEmptyObject
  }

  implicit val copyEncodeJson: EncodeJson[Copy] = EncodeJson[Copy] {
    case Copy(productId, CopyData(barcode, note)) ⇒
      ("productId" := productId) ->: ("barcode" := barcode) ->: ("note" := note) ->: jEmptyObject
  }

  implicit val iccEncodeJson: EncodeJson[InventoryCheckCopy] = EncodeJson[InventoryCheckCopy] {
    case InventoryCheckCopy(Product(_, ProductData(name, _)), Copy(_, CopyData(barcode, __)), status) ⇒
      val b = ("name" := name) ->: ("barcode" := barcode) ->: jEmptyObject
      status match {
        case Borrowed(_) ⇒ ("status" := "loaned") ->: b
        case Available ⇒ ("status" := "unknown") ->: b
      }
  }

  implicit val localDateCodecJson: CodecJson[LocalDate] =
    CodecJson.derived[Long].xmap(Instant.ofEpochMilli(_).atZone(ZoneId.systemDefault()).toLocalDate)(_.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli)

  implicit val personIdCodecJson: CodecJson[PersonId] =
    CodecJson.derived[Long].xmap(PersonId)(_.id)

  implicit val personEncodeJson: EncodeJson[Person] = EncodeJson[Person] {
    case Person(id, firstName, lastName, email) ⇒
      ("id" := id) ->: ("firstName" := firstName) ->: ("lastName" := lastName) ->: ("email" := email) ->: jEmptyObject
  }

  implicit def copyDetailsEncodeJson: EncodeJson[CopyDetails] =
    EncodeJson[CopyDetails] {
      case CopyDetails(product, copy, status) ⇒
        val b = ("product" := product)(productEncodeJson) ->: ("copy" := copy) ->: jEmptyObject
        status match {
          case Available => ("status" := "available") ->: b
          case Borrowed(_) => ("status" := "borrowed") ->: b
        }
    }

  implicit def ongoingEncodeJson: EncodeJson[Ongoing] = EncodeJson[Ongoing] {
    case Ongoing(barcode, borrower, borrowed) ⇒
      ("barcode" := barcode) ->: ("borrower" := borrower) ->: ("borrowed" := borrowed) ->: jEmptyObject
  }

  implicit val returnedEncodeJson: EncodeJson[Returned] = EncodeJson[Returned] {
    case Returned(barcode, borrower, borrowed, returned) ⇒
      ("barcode" := barcode) ->:
        ("borrower" := borrower) ->:
        ("borrowed" := borrowed) ->:
        ("returned" := returned) ->:
        jEmptyObject
  }

  implicit val fileDescriptionEncodeJson: EncodeJson[FileDescription] = EncodeJson[FileDescription] {
    case FileDescription(MetaData(name, contentType, size), fileData) ⇒
      ("name" := name) ->:
        ("contentType" := contentType) ->:
        ("size" := size) ->:
        ("data" := new String(java.util.Base64.getEncoder.encode(fileData), StandardCharsets.UTF_8)) ->:
        jEmptyObject
  }

  implicit val productDataCodec: CodecJson[ProductData] =
    casecodec2(ProductData.apply, ProductData.unapply)("name", "description")

  implicit val productDetailsEncodeJson: EncodeJson[ProductDetails] =
    jencode2L((pd: ProductDetails) ⇒ (pd.product, pd.copies))("product", "copies")

  implicit val copyDataCodec: CodecJson[CopyData] =
    casecodec2(CopyData.apply, CopyData.unapply)("barcode", "note")

  implicit val uploadRequestCodec: CodecJson[UploadRequest] =
    casecodec3(UploadRequest.apply, UploadRequest.unapply)("name", "contentType", "data")

  implicit val d: EntityDecoder[ProductData] = jsonOf[ProductData]

  implicit val c: EntityDecoder[CopyData] = jsonOf[CopyData]

  implicit val b: EntityDecoder[UploadRequest] = jsonOf[UploadRequest]

  import jsonValidation._
  implicit val x: JsonValidator[CopyData] = new JsonValidator[CopyData] {
    override def validate(json: ACursor): ValidationNel[(String, String), CopyData] = {
      val barcode = json.validate[String]("barcode", "Barcode is required")
        .ensure(NonEmptyList("barcode" → "Barcode must not be empty"))(_.nonEmpty)
      val note = json.validate[Option[String]]("note", "Invalid note")
      (barcode |@| note)(CopyData)
    }
  }

  implicit val y: JsonValidator[ProductData] = new JsonValidator[ProductData] {
    override def validate(json: ACursor): ValidationNel[(String, String), ProductData] = {
      val name = json.validate[String]("name", "Name is required")
        .map(_.trim)
        .ensure(NonEmptyList("name" → "Name must not be empty"))(_.nonEmpty)
      val description = json.validate[Option[String]]("note", "Invalid description")
      (name |@| description)(ProductData)
    }
  }

  implicit val z: JsonValidator[LoanRequest] = new JsonValidator[LoanRequest] {
    override def validate(json: ACursor): ValidationNel[(String, String), LoanRequest] = {
      val borrower = json.validate[PersonId]("borrower", "Must specify borrower")
      val barcode = json.validate[String]("barcode", "Must specify copy")
      val borrowed = json.validate[LocalDate]("borrowed", "Must specify borrow date")
      (borrower |@| barcode |@| borrowed)(LoanRequest)
    }
  }

  implicit val lrv: JsonValidator[ReturnRequest] = new JsonValidator[ReturnRequest] {
    override def validate(json: ACursor): ValidationNel[(String, String), ReturnRequest] = {
      val barcode = json.validate[String]("barcode", "Must specify copy")
      val returned = json.validate[LocalDate]("returned", "Must specify return date")
      (barcode |@| returned)(ReturnRequest)
    }
  }

  object jsonValidation {
    trait JsonValidator[A] {
      def validate(json: ACursor): ValidationNel[(String, String), A]
    }

    implicit final class ACursorOps(val cursor: ACursor) {
      def validate[A: DecodeJson](field: JsonField, errorMessage: String) =
        cursor.get[A](field).toOption.toSuccessNel(field → errorMessage)
    }

    def validateAs[A: JsonValidator](request: Request)(f: A ⇒ Task[Response]) =
      request.decode[Json] { json ⇒ implicitly[JsonValidator[A]].validate(json.acursor).fold(toResponse, f) }

    private def toResponse(errors: NonEmptyList[(String, String)]): Task[Response] = {
      val jsonErrors = errors.map { case (field, error) ⇒ field := error }
      val json = Json.obj(jsonErrors.list: _*)
      BadRequest(json)
    }
  }
}
