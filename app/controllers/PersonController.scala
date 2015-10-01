package controllers

import play.api.libs.json._
import play.api.mvc.{Action, Controller, Result}

trait PersonController {
  this: Controller with Interpreter ⇒

  def search(term: String) = InterpretedAction { request ⇒
    daisy.searchPeople(term) map { searchResult ⇒ Ok(Json.toJson(searchResult)) }
  }

  def photo(id: Long) = InterpretedAction { request ⇒
    daisy.getPhoto(id) map { photo ⇒
      photo.fold[Result](NotFound)(Ok(_).withHeaders(CONTENT_TYPE → "image/jpeg"))
    }
  }

  def specificPerson(id: Long) = InterpretedAction { request ⇒
    daisy.getPerson(id) map { person ⇒
      person.fold[Result](NotFound)(p ⇒ Ok(Json.toJson(p)))
    }
  }
}
