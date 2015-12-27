package models

import models.Product.DuplicateName
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import shapeless.contrib.scalacheck._
import test.{InventoryState, InventoryStateInterpreter}

import scalaz.{State, Free}

class ProductSpecification extends Specification with ScalaCheck with DisjunctionMatchers {
  val inventory = implicitly[Inventory[InventoryManagement]]

  "Products" should {
    "be addable to an empty system" in prop { (data: ProductData) ⇒
      val result = evalEmpty(inventory.addProduct(data))

      result must be_\/-.like {
        case Product(_, d) ⇒ d must beEqualTo(data)
      }
    }

    "not allow duplicate names" in prop { (product: Product) ⇒
      val result = eval(inventory.addProduct(product.data), InventoryState(List(product), Nil))

      result must be_-\/(DuplicateName(product))
    }
  }

  def evalEmpty[A](program: Free[InventoryManagement, A]) =
    eval(program, InventoryState.empty)

  def eval[A](program: Free[InventoryManagement, A], inventory: InventoryState) =
    program.foldMap[({type l[a] = State[InventoryState, a]})#l](InventoryStateInterpreter).eval(inventory)
}
