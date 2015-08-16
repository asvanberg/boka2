package test

import models._

import scalaz.{~>, State}
import scalaz.syntax.equal._
import scalaz.std.anyVal._
import scalaz.std.string._

case class InventoryState(products: List[Product], copies: List[Copy])

object InventoryState extends ((List[Product], List[Copy]) ⇒ InventoryState) {
  def empty = InventoryState(Nil, Nil)
}

object InventoryStateInterpreter extends (InventoryManagement ~> (({type λ[α] = State[InventoryState, α]})#λ)) {
  override def apply[A](fa: InventoryManagement[A]): State[InventoryState, A] = State { state ⇒
    fa match {
      case AddProduct(data) =>
        val id = state.products.foldLeft(0L)((acc, p) ⇒ math.max(acc, p.id)) + 1
        val newProduct = Product(id, data)
        (state.copy(products = newProduct :: state.products), newProduct)
      case FindProduct(id) =>
        (state, state.products.find(_.id === id))
      case ListProducts =>
        (state, state.products)
      case UpdateProduct(product, data) =>
        val updatedProduct = product.copy(data = data)
        val newState = state.copy(products = state.products.map { p ⇒
          if (p.id === product.id) updatedProduct
          else p
        })
        (newState, updatedProduct)
      case RemoveProduct(product) =>
        (state.copy(products = state.products.filterNot(_.id === product.id)), ())
      case FindCopy(identifier) =>
        (state, state.copies.find(_.code === identifier))
      case AddCopy(product, data) =>
        val newCopy = Copy(product.id, data)
        (state.copy(copies = newCopy :: state.copies), newCopy)
      case GetCopies(product) =>
        (state, state.copies.filter(_.productId === product.id))
    }
  }
}
