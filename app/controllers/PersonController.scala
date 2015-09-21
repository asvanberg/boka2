package controllers

import play.api.Configuration
import play.api.http.MimeTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest}
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future

trait PersonController {
  this: Controller ⇒
  
  def configuration: Configuration
  
  def client: WSClient

  val Personnummer = "(\\d{6}\\-\\d{4})".r
  def search(term: String) = Action.async {
    def searchParam = term match {
      case Personnummer(personnummer) ⇒ "personnummer" → personnummer
      case _ ⇒ "fullname" → term
    }
    withApi("person") { request ⇒
      request
        .withHeaders(ACCEPT → MimeTypes.JSON)
        .withQueryString(searchParam)
        .get()
        .map { response ⇒
          response.status match {
            case OK ⇒ Ok(response.json)
            case status ⇒ ServiceUnavailable(JsObject(Map("status" → JsNumber(status))))
          }
        }
    }
  }

  def photo(id: Long) = Action.async {
    withApi(s"person/$id/photo") { request ⇒
      request
        .stream()
        .map { case (headers, data) ⇒
          headers.status match {
            case OK ⇒ Ok.chunked(data).withHeaders("Content-Type" → "image/jpeg")
            case _ ⇒ NotFound
          }
        }
    }
  }

  def person = Action { implicit request ⇒
    Ok(views.html.admin.person.index())
  }

  def specificPerson(id: Long) = Action.async {
    withApi(s"person/$id") { request ⇒
      request
        .withHeaders("Accept" → "application/json")
        .get()
        .map { response ⇒
          response.status match {
            case OK ⇒ Ok(response.body)
            case NOT_FOUND ⇒ NotFound
            case status ⇒ ServiceUnavailable(JsObject(Map("status" → JsNumber(status))))
          }
        }
    }
  }

  private lazy val defaultMissingConfiguration = Future.successful(ServiceUnavailable(JsString("Missing Daisy API configuration")))

  private def withApi(path: String)(f: WSRequest ⇒ Future[Result]) = {
    val apiCall = for {
      host ← configuration.getString("daisy.api.host")
      port ← configuration.getInt("daisy.api.port")
      username ← configuration.getString("daisy.api.username")
      password ← configuration.getString("daisy.api.password")
    } yield {
      val request = client.url(s"$host:$port/rest/$path")
        .withAuth(username, password, WSAuthScheme.BASIC)
      f(request)
    }
    apiCall.getOrElse(defaultMissingConfiguration)
  }
}
