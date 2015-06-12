package models


sealed trait InventoryManagement[A]

final case class AddProduct(data: ProductData) extends InventoryManagement[Product]
final case class FindProduct(id: Long) extends InventoryManagement[Option[Product]]
case object ListProducts extends InventoryManagement[List[Product]]
final case class UpdateProduct(product: Product, data: ProductData) extends InventoryManagement[Product]
final case class RemoveProduct(product: Product) extends InventoryManagement[Unit]

final case class FindCopy(identifier: Identifier) extends InventoryManagement[Option[Copy]]
final case class AddCopy(product: Product, data: CopyData) extends InventoryManagement[Copy]
final case class GetCopies(product: Product) extends InventoryManagement[List[Copy]]
