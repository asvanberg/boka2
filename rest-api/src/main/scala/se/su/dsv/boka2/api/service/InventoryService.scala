package se.su.dsv.boka2.api.service

import argonaut.Argonaut._
import models.Copy.IdentifierNotUnique
import models.Product.DuplicateName
import models._
import org.http4s.HttpService
import org.http4s.argonaut._
import org.http4s.dsl._
import org.http4s.headers.Location
import org.http4s.server._
import se.su.dsv.boka2.api._
import se.su.dsv.boka2.api.model._
import _root_.util.free._

import scalaz._
import scalaz.concurrent.Task
import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.bind._

object InventoryService {
  def apply(interpreter: Boka2Op ~> Task): HttpService = HttpService {
    case request@POST -> Root / "product" ⇒
      jsonValidation.validateAs[ProductData](request) { productData ⇒
        Free.runFC(inventory.addProduct(productData))(interpreter) flatMap {
          case -\/(DuplicateName(_)) ⇒
            Conflict(jSingleObject("name", jString("A product with that name already exists")))
          case \/-(product) ⇒
            Created(product.asJson).putHeaders(Location(request.uri / product.id.toString))
        }
      }
    case GET -> Root / "product" / IntVar(id) / "copies" ⇒
      val prg = inventory.findProduct(id) flatMap {
        _ traverseFC inventory.getCopies
      }
      Free.runFC(prg)(interpreter) flatMap {
        case Some(copies) ⇒ Ok(copies.asJson)
        case None ⇒ NotFound()
      }
    case request@PUT -> Root / "product" / IntVar(id) ⇒
      jsonValidation.validateAs[ProductData](request) { productData ⇒
        val prg = for {
          product ← inventory.findProduct(id)
          updateResult ← product traverseFC {
            inventory.updateProduct(_, productData)
          }
        } yield updateResult

        Free.runFC(prg)(interpreter) flatMap {
          case Some(-\/(DuplicateName(_))) ⇒
            Conflict(jSingleObject("name", jString("A product with that name already exists")))
          case Some(\/-(updated)) ⇒ Ok(updated.asJson)
          case None ⇒ NotFound()
        }
      }
    case request@POST -> Root / "product" / IntVar(id) / "copy" ⇒
      request.decode[CopyData] { copyData ⇒
        val prg = for {
          product ← inventory.findProduct(id)
          copy ← product traverseFC {
            inventory.addCopy(_, copyData)
          }
        } yield copy

        Free.runFC(prg)(interpreter) flatMap {
          case Some(-\/(IdentifierNotUnique(_))) ⇒
            Conflict(jSingleObject("barcode", jString("There is another copy with the same barcode")))
          case Some(\/-(added)) ⇒ Ok(added.asJson)
          case None ⇒ NotFound()
        }
      }
    case GET -> Root / "inventoryCheck" ⇒
      val prg = for {
        products ← inventory.listProducts
        result ← products traverseFC { product ⇒
          for {
            copies ← inventory.getCopies(product)
            result ← copies traverseFC { copy ⇒
              for {
                status ← Copy.status[Boka2Op](copy)
              } yield InventoryCheckCopy(product, copy, status)
            }
          } yield result
        }
      } yield Ok(result.flatten.asJson)
      Free.runFC(prg)(interpreter).join
    case request @ POST -> Root / "product" / IntVar(id) / "image" ⇒
      request.decode[UploadRequest] { uploadRequest ⇒
        val data = java.util.Base64.getMimeDecoder.decode(uploadRequest.data)
        val metaData = MetaData(uploadRequest.name, uploadRequest.contentType, data.length)
        val fd = FileDescription(metaData, data)
        val prg = for {
          product ← inventory.findProduct(id)
          result ← product traverseFC { p ⇒ files.storeFile(s"product-${p.id}", fd)}
        } yield result
        Free.runFC(prg)(interpreter) flatMap { _.fold(NotFound())(_ ⇒ Ok()) }
      }
    case GET -> Root / "product" / IntVar(id) / "image" ⇒
      val prg = for {
        product ← inventory.findProduct(id)
        image ← product traverseFC { p ⇒ files.retrieveFile(s"product-${p.id}")}
      } yield image.flatten
      Free.runFC(prg)(interpreter) flatMap {
        case Some(fd) ⇒
          Ok(fd.asJson)
        case None ⇒
          NotFound()
      }
  }
}
