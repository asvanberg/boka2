name := "boka2"

Common.settings

lazy val core = project

lazy val boka2 = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(core)
  .dependsOn(core)

libraryDependencies ++= Seq( jdbc , anorm , cache , ws )

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Nothing, Wart.NonUnitStatements, Wart.AsInstanceOf)

wartremoverExcluded ++= Seq("Routes", "controllers.ref", "_routes_")
