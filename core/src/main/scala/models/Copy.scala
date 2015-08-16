package models

import util.free._

import scalaz.Free.FreeC
import scalaz.\/.{left, right}
import scalaz.\/

final case class Copy(productId: Long, data: CopyData) {
  def code: Identifier = data.code
  def note: Option[String] = data.note
}

final case class CopyData(code: Identifier, note: Option[String])

object Copy extends ((Long, CopyData) ⇒ Copy) {
  final case class IdentifierNotUnique private[models](duplicate: Copy)

  sealed trait Status
  case object Available extends Status
  final case class Borrowed(current: Ongoing) extends Status

  def status[F[_]](copy: Copy)(implicit L: Loans[F]): FreeC[F, Status] =
    L.current(copy) map { _.fold[Status](Available)(Borrowed) }
}
