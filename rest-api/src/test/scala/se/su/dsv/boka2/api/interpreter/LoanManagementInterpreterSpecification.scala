package se.su.dsv.boka2.api.interpreter

import java.time.LocalDate

import doobie.contrib.specs2.analysisspec.AnalysisSpec
import doobie.imports._
import models.{CopyData, Copy, PersonId}
import org.flywaydb.core.Flyway
import org.specs2.mutable.Specification
import LoanManagementInterpreter._

import scalaz.concurrent.Task

class LoanManagementInterpreterSpecification extends Specification with AnalysisSpec {
  val databaseUrl = "jdbc:h2:mem:lmi;DB_CLOSE_DELAY=-1"

  val transactor = DriverManagerTransactor[Task]("org.h2.Driver", databaseUrl)

  val flyway = new Flyway()
  flyway.setDataSource(databaseUrl, "", "")
  flyway.migrate()

  check(queries.insertOngoing("barcode", PersonId(1), LocalDate.now))
  check(queries.insertReturned("barcode", PersonId(1), LocalDate.now, LocalDate.now))
  check(queries.selectOngoing("barcode"))
  check(queries.selectReturned(Copy(1, CopyData("barcode", None))))
  check(queries.deleteOngoing("barcode"))
}
