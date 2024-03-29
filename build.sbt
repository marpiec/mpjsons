import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.6.37"

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishLocal := {}

publishTo := Some("snapshots" at sys.props.getOrElse("repo", default = "."))

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.11.12",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.scalatest" %% "scalatest" % "2.2.6" % Test)