name := "boka2"

Common.settings

lazy val core = project

lazy val boka2 = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb)
  .aggregate(core)
  .dependsOn(core)

resolvers += Resolver.bintrayRepo("scalaz", "releases")
libraryDependencies ++= Seq(
  jdbc,
  evolutions,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  cache,
  ws,
  specs2 % Test,
  "org.specs2" %% "specs2-scalacheck" % "3.6" % "test",
  "org.specs2" %% "specs2-junit" % "3.6" % "test",
  "org.typelevel" %% "shapeless-scalacheck" % "0.4" % "test",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "bootstrap" % "3.3.4",
  "com.google.code.findbugs" % "jsr305" % "1.3.9" % "provided" // This is here to prevent scalac from failing on missing annotations in Guava's BaseEncoding
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Nothing, Wart.NonUnitStatements, Wart.AsInstanceOf, Wart.Any, Wart.Throw)

wartremoverExcluded <+= crossTarget { _ / "routes" / "main" / "router" / "RoutesPrefix.scala" }
wartremoverExcluded <+= crossTarget { _ / "routes" / "main" / "router" / "Routes.scala" }

pipelineStages := Seq(closure, digest, gzip)