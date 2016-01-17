package models

final case class PersonId(id: Long) extends AnyVal

final case class Person(id: PersonId, firstName: String, lastName: String, email: Option[String])
