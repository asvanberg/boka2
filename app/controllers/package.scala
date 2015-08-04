import models.InventoryManagement

import scalaz.Free.FreeC

package object controllers {
  type Boka2[A] = InventoryManagement[A]
  type Program[A] = FreeC[Boka2, A]
}
