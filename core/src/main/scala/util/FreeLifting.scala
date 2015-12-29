package util

import scalaz.{Coyoneda, Free}
import scalaz.Free.FreeC

trait FreeLifting {
  def pure[F[_]]: Helper[F] = new Helper[F]

  class Helper[F[_]] {
    def apply[A](a: A): FreeC[F, A] = Free.freeMonad[({type λ[α] = Coyoneda[F, α]})#λ].point(a)
  }
}
