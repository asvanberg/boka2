import javax.inject.Inject

import controllers.Security
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.Future

object DevGlobal extends WithFilters

class MockShibbolethFilter @Inject()(configuration: Configuration) extends Filter {
  override def apply(f: (RequestHeader) ⇒ Future[Result])(rh: RequestHeader): Future[Result] = {
    val extraHeaders = for {
      name ← configuration.getString(Security.nameHeader)
      principal ← configuration.getString(Security.principalHeader)
    } yield Map(Security.nameHeader → Seq(name), Security.principalHeader → Seq(principal))

    val modifiedHeaders = rh.headers.toMap ++ extraHeaders.getOrElse(Map.empty)
    val modifiedHeader = rh.copy(headers = new Headers {
      override protected val data = modifiedHeaders.toSeq
    })

    f(modifiedHeader)
  }
}