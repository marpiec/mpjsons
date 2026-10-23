import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.7.5"

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
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.15" % Test,
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.15" % Test)

scalacOptions := Seq("-unchecked", "-deprecation")
enablePlugins(JmhPlugin)

// FieldWritersGenerator (Java) uses java.lang.classfile, which the Scala 2.13 compiler cannot read, so it is compiled first
Compile / compileOrder := CompileOrder.JavaThenScala
Test / compileOrder := CompileOrder.Mixed
Jmh / compileOrder := CompileOrder.Mixed

// Copy of 0.7.2 used as the reference implementation by differential tests and benchmarks (src/legacy)
Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "legacy" / "scala"
Jmh / unmanagedSourceDirectories += baseDirectory.value / "src" / "legacy" / "scala"

Global / onLoad := (Global / onLoad).value andThen { state =>
  val javaVersion = sys.props("java.specification.version").takeWhile(_ != '.').toInt
  require(javaVersion >= 25, s"mpjsons requires Java 25 or newer, but build runs on Java $javaVersion")
  state
}
