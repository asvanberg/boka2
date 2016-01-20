import scalaz.Free.FreeC
import scalaz.{Coyoneda, Free, Monad}

package object models {
  type Identifier = String

  implicit def freeCMonad[F[_]]: Monad[FreeC[F, ?]] = Free.freeMonad[Coyoneda[F, ?]]
}
