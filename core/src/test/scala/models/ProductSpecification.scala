package models

import models.Product.DuplicateName
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import shapeless.contrib.scalacheck._

import scalaz.Id.Id

class ProductSpecification extends Specification with ScalaCheck with DisjunctionMatchers {
  "Products" should {
    "be addable to an empty system" in prop { (name: String, description: Option[String]) ⇒
      val add = Product.addM[Id](Nil, data ⇒ Product(1, data.name, data.description)) _

      add(ProductData(name, description)) must be_\/-.like {
        case Product(_, n, d) ⇒ (n must beEqualTo(name)) and (d must beEqualTo(description))
      }
    }

    "not allow duplicate names" in prop { (product: Product) ⇒
      val add = Product.addM[Id](List(product), data ⇒ Product(2, data.name, data.description)) _

      add(ProductData(product.name, product.description)) must be_-\/(DuplicateName(product))
    }
  }
}
