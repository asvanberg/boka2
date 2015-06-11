package models

import scala.collection.immutable._

final case class ProductDetails(product: Product, copies: List[Copy])