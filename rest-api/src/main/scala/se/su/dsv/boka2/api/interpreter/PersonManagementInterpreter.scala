package se.su.dsv.boka2.api.interpreter

import argonaut.Argonaut._
import argonaut.CodecJson
import models._
import org.http4s._
import org.http4s.argonaut._
import org.http4s.headers.{Accept, Authorization}
import org.http4s.Status._
import PersonManagementInterpreter._
import scodec.bits.ByteVector
import se.su.dsv.boka2.api.PersonApiConfiguration

import scalaz._
import scalaz.concurrent.Task
import scalaz.std.option._

class PersonManagementInterpreter(apiConfig: PersonApiConfiguration) extends (PersonManagement ~> Task) with ArgonautInstances with Http4sInstances{
  private val client = org.http4s.client.blaze.defaultClient
  private val authorizationHeader = Authorization(BasicCredentials(apiConfig.username, apiConfig.password))
  private val baseUri = apiConfig.uri

  private def jsonRequest(endpoint: Uri ⇒ Uri) =
    client(
      Request(uri = endpoint(baseUri))
        .putHeaders(
          authorizationHeader,
          Accept(MediaType.`application/json`)
        )
    )

  override def apply[A](fa: PersonManagement[A]): Task[A] =
    fa match {
      case SearchPeople(term) =>
        val searchParameter = term match {
          case Personnummer(birthday, extraDigits) ⇒ "personnummer" → s"$birthday-$extraDigits"
          case AccessCard(number) ⇒ "cardNumber" → number
          case str if str contains "@" ⇒ "email" → str
          case _ ⇒ "fullname" → term
        }
        jsonRequest(_ / "person" +?(searchParameter._1, searchParameter._2)) flatMap {
          case Ok(response) => response.as[List[Person]]
        }
      case GetPerson(id) =>
        jsonRequest(_ / "person" / id.toString) flatMap {
          case Ok(response) => response.as[Person].map(some)
          case NotFound(_) => Task.now(none)
        }
      case GetPhoto(id) =>
        val request = Request(
          uri = baseUri / "person" / id.toString / "photo",
          headers = Headers(authorizationHeader)
        )
        client(request) flatMap {
          case Ok(response) => response.as[ByteVector] map { b => some(b.toArray) }
          case NotFound(_) => Task.now(none)
        }
    }
}

object PersonManagementInterpreter {
  private val Personnummer = "(\\d{6})\\-(\\d{4})".r
  private val AccessCard = "(\\d{3,})".r

  implicit val personIdCodec: CodecJson[PersonId] = CodecJson.derived[Long].xmap(PersonId)(_.id)
  implicit val personCodec: CodecJson[Person] = casecodec4(Person.apply, Person.unapply)("id", "firstName", "lastName", "email")

  implicit def personDecoder: EntityDecoder[Person] = jsonOf[Person]
  implicit def personListDecoder: EntityDecoder[List[Person]] = jsonOf[List[Person]]
}
