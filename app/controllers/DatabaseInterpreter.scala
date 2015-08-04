package controllers

import java.sql.Connection

import anorm._
import anorm.SqlParser
import models._

import scalaz.{Reader, ~>}

object DatabaseInterpreter extends (InventoryManagement ~> ({type λ[α] = Reader[Connection, α]})#λ) {
  override def apply[A](fa: InventoryManagement[A]): Reader[Connection, A] = Reader {
    implicit connection ⇒
      fa match {
        case AddProduct(data@ProductData(name, description)) ⇒
          val id = SQL"""
              INSERT INTO product (id, name, description)
              VALUES (default, $name, $description)
            """.executeInsert(SqlParser.scalar[Long].single)
          Product(id, data)
        case FindProduct(id) ⇒
          SQL"""
              SELECT * FROM product WHERE id = $id
            """.as(Parsers.productParser.singleOpt)
        case ListProducts ⇒
          SQL"""
             SELECT * FROM product
            """.as(Parsers.productParser.*)
        case UpdateProduct(product, data@ProductData(name, description)) ⇒
          val updated = SQL"""
              UPDATE product
              SET name = $name, description = $description
              WHERE id = ${product.id}
            """.executeUpdate()
          product.copy(data = data)
        case RemoveProduct(product) ⇒
          SQL"""
              DELETE FROM product WHERE id = ${product.id}
            """.execute()
          ()
        case AddCopy(product, data@CopyData(identifier, note)) ⇒
          SQL"""
                INSERT INTO copy (identifier, product_id, note)
                VALUES ($identifier, ${product.id}, $note)
             """.executeInsert()
          Copy(product.id, data)
        case FindCopy(identifier) ⇒
          SQL"""
                SELECT * FROM copy WHERE identifier = $identifier
             """.as(Parsers.copyParser.singleOpt)
        case GetCopies(Product(productId, _)) ⇒
          SQL"""
                SELECT * FROM copy WHERE product_id = $productId
             """.as(Parsers.copyParser.*)
      }
  }
}

object Parsers {
  import anorm.SqlParser._
  val productParser = long("id") ~ str("name") ~ str("description").? map {
    case id ~ name ~ description ⇒ Product(id, ProductData(name, description))
  }
  val copyParser = long("product_id") ~ str("identifier") ~ str("note").? map {
    case productId ~ identifier ~ note ⇒ Copy(productId, CopyData(identifier, note))
  }
}