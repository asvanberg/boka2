package models


sealed trait Command[A]

final case class AddProduct(data: ProductData) extends Command[Product]
final case class FindProduct(id: Long) extends Command[Option[Product]]
case object ListProducts extends Command[List[Product]]
final case class UpdateProduct(product: Product, data: ProductData) extends Command[Product]
final case class RemoveProduct(product: Product) extends Command[Unit]

final case class FindCopy(identifier: Identifier) extends Command[Option[Copy]]
final case class AddCopy(product: Product, data: CopyData) extends Command[Copy]
final case class GetCopies(product: Product) extends Command[List[Copy]]
