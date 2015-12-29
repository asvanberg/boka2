package algebra

import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.libs.json.{Format, Json}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Free, Inject, Kleisli, ~>}

sealed trait Daisy[A]
final case class SearchPeople(term: String) extends Daisy[List[Person]]
final case class GetPerson(id: Long) extends Daisy[Option[Person]]
final case class GetPhoto(id: Long) extends Daisy[Option[Array[Byte]]]

final case class Person(id: Long, firstName: String, lastName: String, email: Option[String])
object Person {
  implicit val format: Format[Person] = Json.format[Person]
}

class Daisy0[F[_]](implicit I: Inject[Daisy, F]) {
  type G[A] = Free.FreeC[F, A]

  private def lift[A](a: Daisy[A]): G[A] = Free.liftFC(I.inj(a))

  def getPerson(id: Long) = lift(GetPerson(id))

  def searchPeople(term: String) = lift(SearchPeople(term))

  def getPhoto(id: Long) = lift(GetPhoto(id))
}

object Daisy {
  final case class Configuration(host: String, port: Int, username: String, password: String)

  private val Personnummer = "(\\d{6})\\-(\\d{4})".r
  private val AccessCard = "(\\d{3,})".r

  def Interpreter(implicit ec: ExecutionContext) = new (Daisy ~> ({type λ[α] = Kleisli[Future, (WSClient, Configuration), α]})#λ) {
    override def apply[A](fa: Daisy[A]): Kleisli[Future, (WSClient, Configuration), A] = Kleisli {
      case (client, Configuration(host, port, username, password)) ⇒
        def f(path: String) = client
          .url(s"$host:$port/rest/$path")
          .withAuth(username, password, WSAuthScheme.BASIC)

        fa match {
          case SearchPeople(term) ⇒
            val searchParameter = term match {
              case Personnummer(birthday, extraDigits) ⇒ "personnummer" → s"$birthday-$extraDigits"
              case AccessCard(number) ⇒ "cardNumber" → number
              case email if email contains "@" ⇒ "email" → email
              case _ ⇒ "fullname" → term
            }
            f("person")
              .withHeaders(HeaderNames.ACCEPT → MimeTypes.JSON)
              .withQueryString(searchParameter)
              .get()
              .map(_.json.as[List[Person]])
              .map(_.sortBy(_.firstName))
          case GetPerson(id) ⇒
            f(s"person/$id")
              .withHeaders(HeaderNames.ACCEPT → MimeTypes.JSON)
              .get()
              .map(_.json.asOpt[Person])
          case GetPhoto(id) ⇒
            f(s"person/$id/photo")
              .get()
              .map { response ⇒
                response.status match {
                  case Status.OK ⇒ Some(response.bodyAsBytes)
                  case _ ⇒ None
                }
              }
        }
    }
  }
}