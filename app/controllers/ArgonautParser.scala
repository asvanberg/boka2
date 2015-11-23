package controllers

import play.api.libs.iteratee.{Traversable, Iteratee}
import play.api.mvc.{Results, BodyParser}

object ArgonautParser {

  val MaxBodyLength: Long = 512 * 1024
  type Foo[A] = Array[Byte] => A

  def json[A : Foo]: BodyParser[A] = BodyParser { request =>
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    Traversable.takeUpTo[Array[Byte]](MaxBodyLength).transform(
      Iteratee.consume[Array[Byte]]().map { bytes =>
        implicitly[Foo[A]].apply(bytes)
      }
    ).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
  }

}
