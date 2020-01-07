name := "butcher"
organization := "org.butcher"

scalaVersion := "2.12.10"
val circeVersion = "0.12.3"
scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-language:postfixOps,higherKinds,implicitConversions,existentials", "-unchecked")

// to get types like Reader[String, ?] (with more than one type parameter) correctly inferred for scala 2.12.x
scalacOptions += "-Ypartial-unification"

// to write types like Reader[String, ?]
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

ThisBuild / githubOwner := "mkeshav"
ThisBuild / githubRepository := "butcher"
ThisBuild / githubTokenSource := Some(TokenSource.Environment("GITHUB_TOKEN"))
ThisBuild / githubUser := "mkeshav@gmail.com"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "fastparse" % "2.1.3",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "io.circe" %% "circe-core"% circeVersion,
  "io.circe" %% "circe-generic"% circeVersion,
  "io.circe" %% "circe-parser"% circeVersion,
  "io.circe" %% "circe-java8"% "0.12.0-M1",
  "com.outr" %% "hasher" % "1.2.1",
  //java
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.10.1",
  "com.amazonaws" % "aws-java-sdk-kms" % "1.11.691",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test
)
