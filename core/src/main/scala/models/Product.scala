package models

import models.Execution._

import scala.collection.immutable.List
import scalaz.\/.{left, right}
import scalaz.std.anyVal._
import scalaz.std.string._
import scalaz.syntax.equal._
import scalaz.syntax.monad._
import scalaz.{Monad, \/}

final case class Product(id: Long, name: String, description: Option[String])

final case class ProductData(name: String, description: Option[String])

object Product extends ((Long, String, Option[String]) ⇒ Product) {
  final case class DuplicateName(other: Product)

  def add: (ProductData) ⇒ Program[DuplicateName \/ Product] =
    uniqueName[Program](list, data ⇒ execute(AddProduct(data)))

  def update(product: Product): ProductData ⇒ Program[DuplicateName \/ Product] =
    uniqueName[Program](
      list map { _ filter { _.id /== product.id } },
      data ⇒ execute(UpdateProduct(product, data))
    )

  private[models] def uniqueName[M[_]: Monad]
  (listF: M[List[Product]], g: ProductData ⇒ M[Product])
  (a: ProductData): M[DuplicateName \/ Product] = for {
      products ← listF
      result ← products.find(_.name === a.name) match {
        case Some(duplicate) ⇒ left(DuplicateName(duplicate)).pure[M]
        case None ⇒ g(a) map right
      }
    } yield result

  def list: Program[List[Product]] = execute(ListProducts)

  def find(id: Long): Program[Option[Product]] = execute(FindProduct(id))

}