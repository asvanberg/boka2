package se.su.dsv.boka2.api.model

import models.{Copy, Product}

final case class ProductDetails(product: Product, copies: List[Copy])
