assemblyJarName in assembly := "butcher.jar"

name := "butcher"
organization := "org.butcher"

target in assembly := baseDirectory.value
scalaVersion := "2.12.10"

scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-language:postfixOps,higherKinds,implicitConversions,existentials", "-unchecked")

// to get types like Reader[String, ?] (with more than one type parameter) correctly inferred for scala 2.12.x
scalacOptions += "-Ypartial-unification"

// to write types like Reader[String, ?]
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

ThisBuild / githubOwner := "mkeshav"
ThisBuild / githubRepository := "butcher"
ThisBuild / githubTokenSource := Some(TokenSource.Environment("GITHUB_TOKEN"))
ThisBuild / githubUser := "mkeshav@gmail.com"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "fastparse" % "2.1.3",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "com.outr" %% "hasher" % "1.2.1",
  //java
  "com.amazonaws" % "aws-java-sdk-kms" % "1.11.691",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.10.1" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test
)
