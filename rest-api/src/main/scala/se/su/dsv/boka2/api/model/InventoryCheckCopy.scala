package se.su.dsv.boka2.api.model

import models.{Copy, Product}

final case class InventoryCheckCopy(product: Product, copy: Copy, status: Copy.Status)
