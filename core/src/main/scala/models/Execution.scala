package models

import scalaz.Free
import scalaz.Free.FreeC

sealed trait Execution[A]
final case class Pure[A] private[models] (a: A) extends Execution[A]
final case class Execute[A] private[models] (command: Command[A]) extends Execution[A]

private[models] object Execution {
 def pure[A](a: A): FreeC[Execution, A] =
   Free.liftFC(Pure(a))

 def execute[A](command: Command[A]): FreeC[Execution, A] =
   Free.liftFC(Execute(command))
}
