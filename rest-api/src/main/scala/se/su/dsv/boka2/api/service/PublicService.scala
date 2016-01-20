package se.su.dsv.boka2.api.service

import argonaut.Argonaut._
import org.http4s.argonaut._
import org.http4s.dsl._
import org.http4s.{HttpService, Request, Response}
import se.su.dsv.boka2.api.model.ProductDetails
import se.su.dsv.boka2.api.{Boka2Op, Program, inventory}
import util.free._

import scalaz.concurrent.Task
import scalaz.std.option._
import scalaz.syntax.monad._
import scalaz.{Free, ~>}

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
