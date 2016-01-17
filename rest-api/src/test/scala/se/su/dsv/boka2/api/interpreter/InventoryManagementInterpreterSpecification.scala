package se.su.dsv.boka2.api.interpreter

import doobie.contrib.specs2.analysisspec.AnalysisSpec
import doobie.imports._
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification
import InventoryManagementInterpreter.queries

import scalaz.concurrent.Task

class InventoryManagementInterpreterSpecification extends Specification with AnalysisSpec {
  val databaseUrl = "jdbc:h2:mem:imi;DB_CLOSE_DELAY=-1"

  val transactor = DriverManagerTransactor[Task]("org.h2.Driver", databaseUrl)

  val flyway = new Flyway()
  flyway.setDataSource(databaseUrl, "", "")
  flyway.migrate()

  check(queries.insertProduct("product name", Some("description")))
  check(queries.selectProduct(1))
  check(queries.selectAllProducts)
  check(queries.updateProduct(1, "new product name", None))
  check(queries.deleteProduct(1))
  check(queries.insertCopy(1, "barcode", Some("note")))
  check(queries.selectCopy("barcode"))
  check(queries.selectProductCopies(1))
}
