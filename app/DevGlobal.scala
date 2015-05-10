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
    } yield Seq(Security.nameHeader → name, Security.principalHeader → principal)

    val modifiedHeaders = rh.headers.add(extraHeaders.toSeq.flatten: _*)
    val modifiedHeader = rh.copy(headers = modifiedHeaders)

    f(modifiedHeader)
  }
}