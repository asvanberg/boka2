package models


sealed trait Inventory[A]

final case class AddProduct(data: ProductData) extends Inventory[Product]
final case class FindProduct(id: Long) extends Inventory[Option[Product]]
case object ListProducts extends Inventory[List[Product]]
final case class UpdateProduct(product: Product, data: ProductData) extends Inventory[Product]
final case class RemoveProduct(product: Product) extends Inventory[Unit]

final case class FindCopy(identifier: Identifier) extends Inventory[Option[Copy]]
final case class AddCopy(product: Product, data: CopyData) extends Inventory[Copy]
final case class GetCopies(product: Product) extends Inventory[List[Copy]]
