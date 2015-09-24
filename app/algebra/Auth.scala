package algebra

sealed trait Auth[A]
final case class IsAdmin(sub: String) extends Auth[Boolean]