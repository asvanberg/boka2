package se.su.dsv.boka2.api.service

import argonaut.Argonaut._
import org.http4s.MediaType.`application/base64`
import org.http4s.argonaut._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.server.HttpService
import se.su.dsv.boka2.api._

import scalaz._
import scalaz.concurrent.Task

object PersonService {
  def apply(interpreter: Boka2Op ~> Task): HttpService = HttpService {
    case GET -> Root :? SearchTerm(term) ⇒
      Free.runFC(persons.searchPeople(term))(interpreter) flatMap {
        people ⇒ Ok(people.asJson)
      }
    case GET -> Root / LongVar(id) / "photo" ⇒
      Free.runFC(persons.getPhoto(id))(interpreter) flatMap {
        case Some(photo) ⇒
          Ok(java.util.Base64.getEncoder.encode(photo))
            .putHeaders(`Content-Type`(`application/base64`))
        case None ⇒ NotFound()
      }
    case GET -> Root / LongVar(id) ⇒
      Free.runFC(persons.getPerson(id))(interpreter) flatMap {
        case Some(person) ⇒ Ok(person.asJson)
        case None ⇒ NotFound()
      }
  }

  object SearchTerm extends QueryParamDecoderMatcher[String]("searchTerm")
}
