import scalaz.Free.FreeC
import scalaz.{Coyoneda, Free, Monad}

package object models {
  type Program[A] = FreeC[Execution, A]
  type Identifier = String

  implicit def programMonad: Monad[Program] = Free.freeMonad[({type λ[α] = Coyoneda[Execution, α]})#λ]
}
