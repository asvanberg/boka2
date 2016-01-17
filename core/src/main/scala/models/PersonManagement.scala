package models

import scalaz.{Free, Inject}

sealed trait PersonManagement[A]
final case class SearchPeople(term: String) extends PersonManagement[List[Person]]
final case class GetPerson(id: Long) extends PersonManagement[Option[Person]]
final case class GetPhoto(id: Long) extends PersonManagement[Option[Array[Byte]]]

class Persons[F[_]](implicit I: Inject[PersonManagement, F]) {
  type G[A] = Free.FreeC[F, A]

  private def lift[A](a: PersonManagement[A]): G[A] = Free.liftFC(I.inj(a))

  def getPerson(id: Long) = lift(GetPerson(id))

  def searchPeople(term: String) = lift(SearchPeople(term))

  def getPhoto(id: Long) = lift(GetPhoto(id))
}

object Persons {
  implicit def persons[F[_]](implicit I: Inject[PersonManagement, F]): Persons[F] = new Persons
}