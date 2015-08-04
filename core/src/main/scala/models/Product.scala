package models

import util.free._

import scala.collection.immutable.List
import scalaz.Free.FreeC
import scalaz.\/.{left, right}
import scalaz.std.anyVal._
import scalaz.std.list._
import scalaz.std.string._
import scalaz.syntax.equal._
import scalaz.syntax.monad._
import scalaz.{Monad, NonEmptyList, \/}

final case class Product(id: Long, data: ProductData) {
  def name: String = data.name
  def description: Option[String] = data.description
}

final case class ProductData(name: String, description: Option[String])

object Product extends ((Long, ProductData) ⇒ Product) {
  final case class DuplicateName(other: Product)

  def add[F[_]](data: ProductData)(implicit I: Inventory[F]): FreeC[F, DuplicateName \/ Product] =
    uniqueName[({type λ[α] = FreeC[F, α]})#λ](data.name, list)(I.addProduct(data))

  def update[F[_]](product: Product, data: ProductData)(implicit I: Inventory[F]): FreeC[F, DuplicateName \/ Product] =
    uniqueName[({type λ[α] = FreeC[F, α]})#λ](
      data.name, list map { _ filter { _.id /== product.id } }
    )(I.updateProduct(product, data))

  private[models] def uniqueName[M[_]: Monad](name: String, listM: M[List[Product]])(g: ⇒ M[Product]): M[DuplicateName \/ Product] = for {
      products ← listM
      result ← products.find(_.name === name) match {
        case Some(duplicate) ⇒ left(DuplicateName(duplicate)).pure[M]
        case None ⇒ g map right
      }
    } yield result

  def list[F[_]](implicit I: Inventory[F]): FreeC[F, List[Product]] = I.listProducts

  def find[F[_]](id: Long)(implicit I: Inventory[F]): FreeC[F, Option[Product]] = I.findProduct(id)

  sealed trait Status
  case object NoCopies extends Status
  final case class Unavailable(borrowed: NonEmptyList[(Copy, Ongoing)]) extends Status
  final case class Available(available: NonEmptyList[Copy]) extends Status

  def status[F[_]](product: Product)(implicit I: Inventory[F], L: Loans[F]): FreeC[F, Status] = {
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