name := "sdk-scala-binding"

version := "0.1"

scalaVersion := "2.13.3"

lazy val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "org.slf4j"      % "slf4j-api"       % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3", // TODO make provided
  "org.scalatest"  %% "scalatest"      % "3.2.2" % Test
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

scalacOptions in Compile ++= Seq(
  "-Xlog-reflective-calls",
  "-Xlint",
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Yrangepos"
  // "-Xfatal-warnings"
)
