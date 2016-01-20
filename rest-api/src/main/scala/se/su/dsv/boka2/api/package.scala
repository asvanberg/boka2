package se.su.dsv.boka2

import knobs.{CfgValue, Configured}
import models._

import scalaz.Coproduct
import scalaz.Free._

package object api {
  type F0[A] = Coproduct[InventoryManagement, LoanManagement, A]
  type F1[A] = Coproduct[PersonManagement, F0, A]
  type Boka2Op[A] = Coproduct[FileStore, F1, A]
  type Program[A] = FreeC[Boka2Op, A]

  object inventory extends Inventory[Boka2Op]

  object loans extends Loans[Boka2Op]

  object persons extends Persons[Boka2Op]

  object files extends Files[Boka2Op]

  implicit object JPathConfigured extends Configured[java.nio.file.Path] {
    override def apply(v: CfgValue): Option[java.nio.file.Path] =
      v.convertTo[String]
        .map(java.nio.file.Paths.get(_))
        .filter(java.nio.file.Files.exists(_))
  }
}
