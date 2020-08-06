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
  "com.lihaoyi" %% "fastparse" % "2.2.2",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "io.circe" %% "circe-core"% circeVersion,
  "io.circe" %% "circe-generic"% circeVersion,
  "io.circe" %% "circe-parser"% circeVersion,
  "io.circe" %% "circe-java8"% "0.12.0-M1",
  "com.outr" %% "hasher" % "1.2.1",
  "com.gu" %% "scanamo" % "1.0.0-M8",

  //java
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.711",
  "com.amazonaws" % "aws-java-sdk-kms" % "1.11.691",
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.10.1" % Test,
  "io.circe" %% "circe-yaml" % "0.10.0" % Test,
)
