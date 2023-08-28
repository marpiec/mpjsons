import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.6.40"

scalaVersion := "2.13.11"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishLocal := {}

publishTo := Some("snapshots" at sys.props.getOrElse("repo", default = "."))

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.13.11",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.15" % Test)

scalacOptions := Seq("-unchecked", "-deprecation")