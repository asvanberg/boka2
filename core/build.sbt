name := "core"

Common.settings

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.specs2" %% "specs2-scalacheck" % "3.5" % "test",
  "org.specs2" %% "specs2-junit" % "3.5" % "test",
  "org.typelevel" %% "shapeless-scalacheck" % "0.3" % "test"
)

wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.Nothing, Wart.Any, Wart.Throw, Wart.NoNeedForMonad)
