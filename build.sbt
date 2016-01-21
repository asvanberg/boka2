lazy val commonSettings = Seq(
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",       // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  ),
  wartremoverErrors in (Compile, compile) ++= Warts.allBut(
    Wart.Nothing,
    Wart.NonUnitStatements,
    Wart.AsInstanceOf,
    Wart.Any,
    Wart.Throw,
    Wart.ExplicitImplicitTypes
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
)

val scalazVersion = "7.1.6"
val http4sVersion = "0.12.0"
val doobieVersion = "0.2.3"

lazy val boka2 = (project in file("."))
  .settings(commonSettings)
  .aggregate(core, api)
  .dependsOn(core, api)

lazy val core = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion,
      "org.specs2" %% "specs2-scalacheck" % "3.6.6" % Test,
      "org.typelevel" %% "shapeless-scalacheck" % "0.3" % Test
    )
  )

lazy val api = project.in(file("rest-api"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(resolvers += Resolver.bintrayRepo("oncue", "releases"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion,
      "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-argonaut" % http4sVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-contrib-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-contrib-specs2" % doobieVersion % Test,
      "com.h2database" % "h2" % "1.4.190",
      "org.postgresql" % "postgresql" % "9.4.1207",
      "io.argonaut" %% "argonaut" % "6.1",
      "oncue.knobs" %% "core" % "3.3.3",
      "org.slf4j" % "slf4j-simple" % "1.7.12",
      "org.flywaydb" % "flyway-core" % "3.2.1",
      "org.specs2" %% "specs2-core" % "3.6.6" % Test
    )
  )
