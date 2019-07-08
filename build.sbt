import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.6.29"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishLocal := {}

publishTo := Some("snapshots" at sys.props.getOrElse("snapshotsRepo", default = "http://vps120320.ovh.net:9081/nexus/content/repositories/jtweston-releases/"))

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.11.7",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test)