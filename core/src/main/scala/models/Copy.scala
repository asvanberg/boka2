package models

import models.Execution._

import scalaz.\/.{left, right}
import scalaz.{\/, Monad}
import scalaz.syntax.monad._

final case class Copy(productId: Long, data: CopyData) {
  def code: Identifier = data.code
  def note: Option[String] = data.note
}

final case class CopyData(code: Identifier, note: Option[String])

object Copy extends ((Long, CopyData) ⇒ Copy) {
  final case class IdentifierNotUnique private[models](duplicate: Copy)

  def find(code: Identifier): Program[Option[Copy]] = execute(FindCopy(code))

  def add: (Product, CopyData) ⇒ Program[IdentifierNotUnique \/ Copy] =
    addM[Program](find, (product, data) ⇒ execute(AddCopy(product, data)))

  private[models] def addM[M[_] : Monad](
    findF: Identifier ⇒ M[Option[Copy]],
    addCopyF: (Product, CopyData) ⇒ M[Copy]
  )(product: Product, data: CopyData): M[IdentifierNotUnique \/ Copy] = for {
    existing ← findF(data.code)
    result ← existing match {
      case Some(duplicate) ⇒ left(IdentifierNotUnique(duplicate)).pure[M]
      case None ⇒ addCopyF(product, data) map right
    }
  } yield result
}
