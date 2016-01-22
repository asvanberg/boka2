package models

import util.free._

import scala.collection.immutable.List
import scalaz.Free.FreeC
import scalaz.NonEmptyList
import scalaz.std.list._
import scalaz.syntax.functor._

final case class Product(id: Int, data: ProductData) {
  def name: String = data.name
  def description: Option[String] = data.description
}

final case class ProductData(name: String, description: Option[String])

object Product extends ((Int, ProductData) ⇒ Product) {
  final case class DuplicateName(other: Product)

  sealed trait Status
  case object NoCopies extends Status
  final case class Unavailable(borrowed: NonEmptyList[(Copy, Ongoing)]) extends Status
  final case class Available(available: NonEmptyList[Copy]) extends Status

  def status[F[_]](product: Product)(implicit I: Inventory[F], L: Loans[F]): FreeC[F, Status] = {
    def deriveProductStatus(statuses: List[(Copy, Copy.Status)]): Status = {
      statuses.foldLeft[Status](NoCopies) {
        case (NoCopies, (copy, Copy.Available)) ⇒ Available(NonEmptyList(copy))
        case (NoCopies, (copy, Copy.Borrowed(ongoing))) ⇒ Unavailable(NonEmptyList(copy → ongoing))
        case (Available(copies), (copy, Copy.Available)) ⇒ Available(copy <:: copies)
        case (Unavailable(_), (copy, Copy.Available)) ⇒ Available(NonEmptyList(copy))
        case (Unavailable(copies), (copy, Copy.Borrowed(ongoing))) ⇒ Unavailable((copy → ongoing) <:: copies)
        case (productStatus, _) ⇒ productStatus
      }
    }

    for {
      copies ← I.getCopies(product)
      statuses ← copies.traverseFC(copy ⇒ Copy.status(copy) strengthL copy)
    } yield {
      deriveProductStatus(statuses)
    }
  }
}