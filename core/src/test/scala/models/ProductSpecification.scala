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
      val add = Product.uniqueName[Id](Nil, data ⇒ Product(1, data)) _

      add(data) must be_\/-.like {
        case Product(_, d) ⇒ d must beEqualTo(data)
      }
    }

    "not allow duplicate names" in prop { (product: Product) ⇒
      val add = Product.uniqueName[Id](List(product), data ⇒ Product(2, data)) _

      add(ProductData(product.name, product.description)) must be_-\/(DuplicateName(product))
    }
  }
}
