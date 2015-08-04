package models

import models.Product.DuplicateName
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import shapeless.contrib.scalacheck._

import scalaz.Id.Id

class ProductSpecification extends Specification with ScalaCheck with DisjunctionMatchers {
  "Products" should {
    "be addable to an empty system" in prop { (data: ProductData) ⇒
      val result = Product.uniqueName[Id](data.name, Nil)(Product(1, data))

      result must be_\/-.like {
        case Product(_, d) ⇒ d must beEqualTo(data)
      }
    }

    "not allow duplicate names" in prop { (product: Product) ⇒
      val result = Product.uniqueName[Id](product.name, List(product))(Product(2, product.data))

      result must be_-\/(DuplicateName(product))
    }
  }
}
