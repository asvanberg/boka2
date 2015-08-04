name := "boka2"

Common.settings

lazy val core = project

lazy val boka2 = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(core)
  .dependsOn(core)

resolvers += Resolver.bintrayRepo("scalaz", "releases")
libraryDependencies ++= Seq(
  jdbc,
  evolutions,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  cache,
  ws,
  specs2 % Test,
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "bootstrap" % "3.3.4"
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Nothing, Wart.NonUnitStatements, Wart.AsInstanceOf, Wart.Any, Wart.Throw)

wartremoverExcluded <+= crossTarget { _ / "routes" / "main" / "router" / "RoutesPrefix.scala" }
wartremoverExcluded <+= crossTarget { _ / "routes" / "main" / "router" / "Routes.scala" }
