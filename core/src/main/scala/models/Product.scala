package models

import util.free._

import scala.collection.immutable.List
import scalaz.{Free, NonEmptyList}
import scalaz.std.list._

final case class Product(id: Long, data: ProductData) {
  def name: String = data.name
  def description: Option[String] = data.description
}

final case class ProductData(name: String, description: Option[String])

object Product extends ((Long, ProductData) ⇒ Product) {
  final case class DuplicateName(other: Product)

  sealed trait Status
  case object NoCopies extends Status
  final case class Unavailable(borrowed: NonEmptyList[(Copy, Ongoing)]) extends Status
  final case class Available(available: NonEmptyList[Copy]) extends Status

  def status[F[_]](product: Product)(implicit I: Inventory[F], L: Loans[F]): Free[F, Status] = {
    def deriveProductStatus(statuses: List[(Copy, Copy.Status)]): Status = {
      statuses.foldLeft[Status](NoCopies) { case (productStatus, (copy, copyStatus)) ⇒
        (productStatus, copyStatus) match {
          case (NoCopies, Copy.Available) ⇒ Available(NonEmptyList(copy))
          case (NoCopies, Copy.Borrowed(ongoing)) ⇒ Unavailable(NonEmptyList(copy → ongoing))
          case (Available(copies), Copy.Available) ⇒ Available(copy <:: copies)
          case (Unavailable(_), Copy.Available) ⇒ Available(NonEmptyList(copy))
          case (Unavailable(copies), Copy.Borrowed(ongoing)) ⇒ Unavailable((copy → ongoing) <:: copies)
          case _ ⇒ productStatus
        }
      }
    }

    def withStatus(copy: Copy) = Copy.status(copy) map { (copy, _) }

    for {
      copies ← I.getCopies(product)
      statuses ← copies.traverseFC(withStatus)
    } yield {
      deriveProductStatus(statuses)
    }
  }
}