package controllers

import play.api.libs.json._
import play.api.mvc.{Controller, Result}

trait PersonController {
  this: Controller with Interpreter with JWTSecurity ⇒

  def search(term: String) = authenticated { jwt ⇒
    InterpretedAction { request ⇒
      isAdmin(jwt) {
        daisy.searchPeople(term) map { searchResult ⇒ Ok(Json.toJson(searchResult)) }
      }
    }
  }

  def photo(id: Long) = authenticated { jwt ⇒
    InterpretedAction { request ⇒
      isAdmin(jwt) {
        daisy.getPhoto(id) map { photo ⇒
          photo.fold[Result](NotFound) { data ⇒
            val encoded = com.google.common.io.BaseEncoding.base64().encode(data)
            Ok(encoded).withHeaders(CONTENT_TYPE → "image/jpeg", CONTENT_TRANSFER_ENCODING → "base64")
          }
        }
      }
    }
  }

  def specificPerson(id: Long) = authenticated { jwt ⇒
    InterpretedAction { request ⇒
      isAdmin(jwt) {
        daisy.getPerson(id) map { person ⇒
          person.fold[Result](NotFound)(p ⇒ Ok(Json.toJson(p)))
        }
      }
    }
  }
}
