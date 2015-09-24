import algebra.Auth
import models.{LoanManagement, Inventory, InventoryManagement}

import scalaz.Coproduct
import scalaz.Free.FreeC

package object controllers {
  type F0[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type Boka2[A] = Coproduct[Auth, F0, A]
  type Program[A] = FreeC[Boka2, A]

  object inventory extends Inventory[Boka2]
}
