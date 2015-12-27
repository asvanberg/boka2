name := "core"

Common.settings

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.0",
  "org.specs2" %% "specs2-scalacheck" % "3.6.6-scalaz-7.2.0" % "test",
  "org.specs2" %% "specs2-junit" % "3.6.6-scalaz-7.2.0" % "test",
  "org.typelevel" %% "shapeless-scalacheck" % "0.3" % "test"
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.Nothing, Wart.Any, Wart.Throw, Wart.NoNeedForMonad)
