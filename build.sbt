organization := "me.lessis"

name := "porteurbars"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "me.lessis" %% "tugboat" % "0.1.0",
  "com.github.jknack" % "handlebars" % "1.3.2"
)
