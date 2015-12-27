package util

import scalaz.Free._
import scalaz.{Free, Traverse}

trait FreeTraversal {
  implicit class TraverseFCUtil[A, F[_] : Traverse](fa: F[A]) {
    def traverseFC[G[_]] = new Helper[G]

    class Helper[G[_]] {
      def apply[B](f: A ⇒ Free[G, B]): Free[G, F[B]] = {
        Traverse[F].traverse[({type λ[α] = Free[G, α]})#λ, A, B](fa)(f)
      }
    }
  }
}
