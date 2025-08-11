import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.6.50"

scalaVersion := "2.13.16"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishLocal := {}

publishTo := Some("snapshots" at sys.props.getOrElse("repo", default = "."))

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.13.16",
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.15" % Test)

scalacOptions := Seq("-unchecked", "-deprecation")