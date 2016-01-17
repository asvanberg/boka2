package models

sealed trait Auth[A]
final case class IsAdmin(subject: String) extends Auth[Boolean]