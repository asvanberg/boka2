package se.su.dsv.boka2.api.service

import argonaut._, Argonaut._
import org.http4s.{Response, Request}
import org.http4s.HttpService
import org.http4s.argonaut._
import org.http4s.dsl._
import se.su.dsv.boka2.api.{Program, inventory, Boka2Op}
import se.su.dsv.boka2.api.model.ProductDetails
import util.free._

import scalaz.concurrent.Task
import scalaz.syntax.monad._
import scalaz.{Free, ~>}
import scalaz.std.option._

object PublicService {
  def apply(interpreter: Boka2Op ~> Task) = freeService(interpreter) {
      case GET -> Root / "product" ⇒
        inventory.listProducts.map(products ⇒ Ok(products.asJson))
      case GET -> Root / "product" / IntVar(id) ⇒
        for {
          product ← inventory.findProduct(id)
          copies ← product traverseFC inventory.getCopies
        } yield ^(product, copies)(ProductDetails) match {
          case Some(productDetails) ⇒ Ok(productDetails.asJson)
          case None ⇒ NotFound()
        }
  }

  private def freeService(interpreter: Boka2Op ~> Task)(pf: PartialFunction[Request, Program[Task[Response]]]): HttpService = {
    HttpService { pf.andThen(Free.runFC(_)(interpreter).join) }
  }
}
