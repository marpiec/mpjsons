import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.5.8-SNAPSHOT"

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
  "-feature")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := Some("snapshots" at sys.props.getOrElse("mavenRepo", default = "http://someMockRepo.com"))

publishLocal := {}

libraryDependencies ++= Seq(
   "org.scala-lang" % "scala-reflect" % "2.11.5",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.testng" % "testng" % "6.8" % Test,
  "org.scalatest" %% "scalatest" % "2.2.2" % Test)