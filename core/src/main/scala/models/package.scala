import scalaz.Free.FreeC
import scalaz.{Coyoneda, Free, Monad}

package object models {
  type Identifier = String

  implicit def freeCMonad[F[_]]: Monad[({type λ[α] = FreeC[F, α]})#λ] = Free.freeMonad[({type λ[α] = Coyoneda[F, α]})#λ]
}
