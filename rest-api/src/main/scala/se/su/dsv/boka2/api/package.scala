package se.su.dsv.boka2

import models._

import scalaz.Coproduct
import scalaz.Free._

package object api {
  type F0[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type Boka2Op[A] = Coproduct[PersonManagement, F0, A]
  type Program[A] = FreeC[Boka2Op, A]

  object inventory extends Inventory[Boka2Op]

  object loans extends Loans[Boka2Op]

  object persons extends Persons[Boka2Op]
}
