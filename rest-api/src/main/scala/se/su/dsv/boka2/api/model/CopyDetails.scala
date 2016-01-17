package se.su.dsv.boka2.api.model

import models.{Copy, Product}

final case class CopyDetails(product: Product, copy: Copy, status: Copy.Status)
