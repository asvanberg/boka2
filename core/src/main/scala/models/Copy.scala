package models

import scalaz.Free.FreeC

final case class Copy(productId: Int, data: CopyData) {
  def code: Identifier = data.code
  def note: Option[String] = data.note
}

final case class CopyData(code: Identifier, note: Option[String])

object Copy extends ((Int, CopyData) ⇒ Copy) {
  final case class IdentifierNotUnique private[models](duplicate: Copy)

  sealed trait Status
  case object Available extends Status
  final case class Borrowed(current: Ongoing) extends Status

  def status[F[_]](copy: Copy)(implicit L: Loans[F]): FreeC[F, Status] =
    L.current(copy) map { _.fold[Status](Available)(Borrowed) }
}
