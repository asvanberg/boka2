logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-M2")

addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.11")
