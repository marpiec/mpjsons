import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.6.8"

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := Some("snapshots" at sys.props.getOrElse("snapshotsRepo", default = "http://someMockRepo.com"))

publishLocal := {}

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.11.5",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test)