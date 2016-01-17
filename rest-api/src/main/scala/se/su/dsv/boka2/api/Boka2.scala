package se.su.dsv.boka2.api

import java.nio.charset.StandardCharsets
import java.util.Base64

import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import doobie.imports._
import knobs._
import org.flywaydb.core.Flyway
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Router, HttpService, Server}
import org.http4s.{DecodeFailureException, Uri}
import se.su.dsv.boka2.api.interpreter.PersonManagementInterpreter
import se.su.dsv.boka2.api.middleware.JWTAuthentication
import se.su.dsv.boka2.api.util.{HS256Signer, jwt}
import jwt._
import se.su.dsv.boka2.api.service.{LoanService, PersonService, InventoryService, PublicService}

import scala.util.Properties._
import scalaz.concurrent.Task
import scalaz.~>

object Boka2 extends App {
  val port = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080

  val app = for {
    config         ← readConfig
    (dbConfig, daisyConfig, jwtSecret) = config
    _              ← migrateDatabase(dbConfig)
    connectionPool ← HikariTransactor[Task](dbConfig.driver, dbConfig.url, "", "")
    interpreter    = buildInterpreter(connectionPool.trans, daisyConfig)
    authentication = new JWTAuthentication(new HS256Signer(jwtSecret))(isAdmin(connectionPool))
    publicService  = PublicService(interpreter)
    adminService   = authentication(Router(
      "/person" → PersonService(interpreter),
      "/copy"   → LoanService(interpreter),
      "/"       → InventoryService(interpreter)
    ))
    server         ← buildServer(adminService, publicService)
  } yield server

  app.run.awaitShutdown()

  def isAdmin(xa: Transactor[Task]): String ⇒ Task[Boolean] = subject ⇒
    sql"SELECT EXISTS (SELECT 1 FROM admins WHERE subject = $subject) OR NOT EXISTS (SELECT 1 FROM admins)"
      .query[Boolean]
      .unique
      .transact(xa)

  def readConfig = {
    def parseUri(str: String): Task[Uri] =
      Uri.fromString(str).fold(
        parseFailure ⇒ Task.fail(new DecodeFailureException(parseFailure)),
        Task.now
      )

    def decode(str: Base64String) =
      Task.delay(Base64.getDecoder.decode(str.getBytes(StandardCharsets.UTF_8)))

    for {
      config ← knobs.loadImmutable(Required(ClassPathResource("application.cfg")) :: Nil)
      databaseDriver = config.require[String]("database.driver")
      databaseUrl = config.require[String]("database.url")
      personsConfig = config.subconfig("api.persons")
      personsUri ← parseUri(personsConfig.require[String]("uri"))
      personsUsername = personsConfig.require[String]("username")
      personsPassword = personsConfig.require[String]("password")
      jwtSecret ← decode(config.require[String]("jwt.secret"))
    } yield {
      (DatabaseConfiguration(databaseDriver, databaseUrl), PersonApiConfiguration(personsUri, personsUsername, personsPassword), jwtSecret)
    }
  }

  def migrateDatabase(dbConfig: DatabaseConfiguration) = Task.delay {
    val flyway = new Flyway()
    flyway.setDataSource(dbConfig.url, "", "")
    flyway.migrate()
  }

  def buildInterpreter(transactor: ConnectionIO ~> Task, daisyConfig: PersonApiConfiguration) = {
    val f0 = transactor compose new (F0 ~> ConnectionIO) {
      override def apply[A](fa: F0[A]): ConnectionIO[A] =
        fa.run.fold(interpreter.InventoryManagementInterpreter.apply, interpreter.LoanManagementInterpreter.apply)
    }
    new (Boka2Op ~> Task) {
      override def apply[A](fa: Boka2Op[A]): Task[A] =
        fa.run.fold(new PersonManagementInterpreter(daisyConfig).apply, f0.apply)
    }
  }

  def buildServer(services: HttpService*): Task[Server] = {
    val server = BlazeBuilder
      .bindLocal(port)
    services.foldLeft(server)(_.mountService(_, "/api")).start
  }
}

final case class DatabaseConfiguration(driver: String, url: String)

final case class PersonApiConfiguration(uri: Uri, username: String, password: String)
