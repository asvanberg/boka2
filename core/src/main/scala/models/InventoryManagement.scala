package models

import scala.collection.immutable.List
import scalaz.Free._
import scalaz.\/.{left, right}
import scalaz.std.anyVal._
import scalaz.std.string._
import scalaz.syntax.equal._
import scalaz.syntax.monad._
import scalaz.{Free, Inject, Monad, \/}

sealed trait InventoryManagement[A]

final case class AddProduct private[models] (data: ProductData) extends InventoryManagement[Product]
final case class FindProduct private[models] (id: Int) extends InventoryManagement[Option[Product]]
case object ListProducts extends InventoryManagement[List[Product]]
final case class UpdateProduct private[models] (product: Product, data: ProductData) extends InventoryManagement[Product]
final case class RemoveProduct private[models] (product: Product) extends InventoryManagement[Unit]

final case class FindCopy private[models] (identifier: Identifier) extends InventoryManagement[Option[Copy]]
final case class AddCopy private[models] (product: Product, data: CopyData) extends InventoryManagement[Copy]
final case class GetCopies private[models] (product: Product) extends InventoryManagement[List[Copy]]

class Inventory[F[_]](implicit I: Inject[InventoryManagement, F]) {
  type G[A] = Free.FreeC[F, A]

  private def lift[A](a: InventoryManagement[A]): G[A] = Free.liftFC(I.inj(a))

  def addProduct(data: ProductData): G[Product.DuplicateName \/ Product] =
    uniqueName[G](data.name, listProducts)(lift(AddProduct(data)))

  def findProduct(id: Int): G[Option[Product]] = lift(FindProduct(id))

  def updateProduct(product: Product, data: ProductData): G[Product.DuplicateName \/ Product] =
    uniqueName[G](data.name, listProducts.map(_.filter(_.id /== product.id)))(lift(UpdateProduct(product, data)))

  private def uniqueName[M[_]: Monad](name: String, productsM: M[List[Product]])(g: ⇒ M[Product]): M[Product.DuplicateName \/ Product] = for {
    products ← productsM
    result ← products.find(_.name === name) match {
      case Some(duplicate) ⇒ left(Product.DuplicateName(duplicate)).pure[M]
      case _ ⇒ g map right
    }
  } yield result

  def listProducts: G[List[Product]] = lift(ListProducts)

  def addCopy(product: Product, data: CopyData): G[Copy.IdentifierNotUnique \/ Copy] = for {
    existing ← findCopy(data.code)
    result ← existing match {
      case Some(duplicate) ⇒ left(Copy.IdentifierNotUnique(duplicate)).pure[G]
      case _ ⇒ lift(AddCopy(product, data)) map right
    }
  } yield result

  def getCopies(product: Product): G[List[Copy]] = lift(GetCopies(product))

  def findCopy(identifier: Identifier): G[Option[Copy]] = lift(FindCopy(identifier))
}

object Inventory {
  implicit def inventory[F[_]](implicit I: Inject[InventoryManagement, F]): Inventory[F] = new Inventory
}