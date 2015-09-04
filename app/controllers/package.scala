import models.{LoanManagement, Inventory, InventoryManagement}

import scalaz.Coproduct
import scalaz.Free.FreeC

package object controllers {
  type Boka2[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type Program[A] = FreeC[Boka2, A]

  object inventory extends Inventory[Boka2]
}
