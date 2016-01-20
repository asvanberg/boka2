package util

import scalaz.Free._
import scalaz.{Coyoneda, Free, Traverse}

trait FreeTraversal {
  implicit class TraverseFCUtil[A, F[_] : Traverse](fa: F[A]) {
    def traverseFC[G[_]] = new Helper[G]

    class Helper[G[_]] {
      def apply[B](f: A ⇒ FreeC[G, B]): FreeC[G, F[B]] = {
        val q = Free.freeMonad[Coyoneda[G, ?]]
        Traverse[F].traverse[FreeC[G, ?], A, B](fa)(f)(q)
      }
    }
  }
}
