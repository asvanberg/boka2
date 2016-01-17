package se.su.dsv.boka2.api.interpreter

import doobie.imports._
import models.{freeCMonad ⇒ _, _}

import scalaz._
import scalaz.syntax.functor._

object InventoryManagementInterpreter extends (InventoryManagement ~> ConnectionIO) {
  override def apply[A](fa: InventoryManagement[A]): ConnectionIO[A] = fa match {
    case AddProduct(data@ProductData(name, description)) ⇒
      queries.insertProduct(name, description)
        .withUniqueGeneratedKeys[Int]("id")
        .map(Product(_, data))
    case FindProduct(id) ⇒
      queries.selectProduct(id)
        .option
    case ListProducts ⇒
      queries.selectAllProducts
        .list
    case UpdateProduct(product, data@ProductData(name, description)) ⇒
      queries.updateProduct(product.id, name, description)
        .run
        .as(product.copy(data = data))
    case RemoveProduct(product) ⇒
      queries.deleteProduct(product.id)
        .run
        .void
    case AddCopy(product, data@CopyData(identifier, note)) ⇒
      queries.insertCopy(product.id, identifier, note)
        .run
        .as(Copy(product.id, data))
    case FindCopy(identifier) ⇒
      queries.selectCopy(identifier)
        .option
    case GetCopies(Product(productId, _)) ⇒
      queries.selectProductCopies(productId)
        .list
  }

  object queries {
    def insertProduct(name: String, description: Option[String]): Update0 =
      sql"""INSERT INTO product (id, name, description)
            VALUES (default, $name, $description)"""
        .update

    def selectProduct(id: Int): Query0[Product] =
      sql"SELECT id, name, description FROM product WHERE id = $id"
        .query[Product]

    def selectAllProducts: Query0[Product] =
      sql"SELECT id, name, description FROM product"
        .query[Product]

    def updateProduct(productId: Int, name: String, description: Option[String]): Update0 =
      sql"""UPDATE product
            SET name = $name, description = $description
            WHERE id = $productId"""
        .update

    def deleteProduct(productId: Int): Update0 =
      sql"DELETE FROM product WHERE id = $productId"
        .update

    def insertCopy(productId: Int, identifier: Identifier, note: Option[String]): Update0 =
      sql"""INSERT INTO copy (identifier, product_id, note)
            VALUES ($identifier, $productId, $note)"""
        .update

    def selectCopy(identifier: Identifier): Query0[Copy] =
      sql"""SELECT product_id, identifier, note
            FROM copy WHERE identifier = $identifier"""
        .query[Copy]

    def selectProductCopies(productId: Int): Query0[Copy] =
      sql"""SELECT product_id, identifier, note
            FROM copy WHERE product_id = $productId"""
        .query[Copy]
  }
}
