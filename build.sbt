import sbt.Keys._

name := "mpjsons"

organization := "io.mpjsons"

version := "0.7.8"

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
// sbt-jmh adds JMH to the Compile dependencies, which would put it into the published pom (and on users' runtime
// classpath); only the benchmarks (src/jmh) need it
libraryDependencies := libraryDependencies.value.map { module =>
  if (module.organization == "org.openjdk.jmh") module.withConfigurations(Some(Jmh.name + "->default(compile)")) else module
}
// sbt-jmh runs its generator and the benchmarks with the Compile classpath; add the JMH artifacts back for them
Jmh / dependencyClasspath := (Compile / dependencyClasspath).value ++ (Jmh / managedClasspath).value

// FieldWritersGenerator (Java) uses java.lang.classfile, which the Scala 2.13 compiler cannot read, so it is compiled first
Compile / compileOrder := CompileOrder.JavaThenScala
// Scaladoc cannot read those Java sources either: document the Scala sources only, against the compiled classes
Compile / doc / sources := (Compile / doc / sources).value.filterNot(_.getName.endsWith(".java"))
Compile / doc / dependencyClasspath ++= (Compile / exportedProducts).value
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
