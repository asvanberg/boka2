import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = Seq(
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    resolvers += Resolver.bintrayRepo("scalaz", "releases"),
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
    )
  )
}
