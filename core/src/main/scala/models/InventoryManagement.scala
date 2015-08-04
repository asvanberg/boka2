package models

import scala.collection.immutable.List
import scalaz.{Free, Inject}

sealed trait InventoryManagement[A]

final case class AddProduct(data: ProductData) extends InventoryManagement[Product]
final case class FindProduct(id: Long) extends InventoryManagement[Option[Product]]
case object ListProducts extends InventoryManagement[List[Product]]
final case class UpdateProduct(product: Product, data: ProductData) extends InventoryManagement[Product]
final case class RemoveProduct(product: Product) extends InventoryManagement[Unit]

final case class FindCopy(identifier: Identifier) extends InventoryManagement[Option[Copy]]
final case class AddCopy(product: Product, data: CopyData) extends InventoryManagement[Copy]
final case class GetCopies(product: Product) extends InventoryManagement[List[Copy]]

class Inventory[F[_]](implicit I: Inject[InventoryManagement, F]) {
  type G[A] = Free.FreeC[F, A]

  private def lift[A](a: InventoryManagement[A]): G[A] = Free.liftFC(I.inj(a))

  def addProduct(data: ProductData): G[Product] = lift(AddProduct(data))

  def findProduct(id: Long): G[Option[Product]] = lift(FindProduct(id))

  def updateProduct(product: Product, data: ProductData): G[Product] = lift(UpdateProduct(product, data))

  def listProducts: G[List[Product]] = lift(ListProducts)

  def addCopy(product: Product, data: CopyData): G[Copy] = lift(AddCopy(product, data))

  def getCopies(product: Product): G[List[Copy]] = lift(GetCopies(product))

  def findCopy(identifier: Identifier): G[Option[Copy]] = lift(FindCopy(identifier))
}

object Inventory {
  implicit def inventory[F[_]](implicit I: Inject[InventoryManagement, F]): Inventory[F] = new Inventory
}