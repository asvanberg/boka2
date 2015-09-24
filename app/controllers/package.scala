import algebra.{Daisy0, Daisy, Auth}
import models.{LoanManagement, Inventory, InventoryManagement}

import scalaz.Coproduct
import scalaz.Free.FreeC

package object controllers {
  type F0[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type F1[A] = Coproduct[Auth, F0, A]
  type Boka2[A] = Coproduct[Daisy, F1, A]
  type Program[A] = FreeC[Boka2, A]

  object inventory extends Inventory[Boka2]

  object daisy extends Daisy0[Boka2]
}
