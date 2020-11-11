name := "sdk-scala-binding"

version := "0.1"

scalaVersion := "2.12.12"

lazy val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "org.slf4j"      % "slf4j-api"       % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test, // TODO make provided
  "org.scalatest"  %% "scalatest"      % "3.2.2" % Test
)

libraryDependencies ++= Seq("circe-core", "circe-generic", "circe-parser", "circe-literal").map("io.circe" %% _ % circeVersion)

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

fork in Test := true

envVars in Test := Map("LD_LIBRARY_PATH" -> (baseDirectory.value / "lib").getPath)
