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
  "com.typesafe.play" %% "anorm" % "2.4.0-RC2",
  cache,
  ws,
  specs2 % Test
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Nothing, Wart.NonUnitStatements, Wart.AsInstanceOf)

wartremoverExcluded ++= Seq("router")
