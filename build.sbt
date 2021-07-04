name := "freeton-sdk-client-scala-binding"

version := "1.18.0-M1"

organization := "com.dancingcode"

lazy val scala212 = "2.12.12"
lazy val scala213 = "2.13.3"

scalaVersion := scala212

// lazy val crossScalaVersions = List(scala212, scala213)

libraryDependencies ++= Seq(
  "org.slf4j"      % "slf4j-api"       % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Provided,
  "org.scalatest"  %% "scalatest"      % "3.2.2" % Test
)

libraryDependencies ++= Seq("circe-core", "circe-generic", "circe-parser", "circe-literal", "circe-generic-extras").map("io.circe" %% _ % "0.13.0")

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
)

fork in Test := true

// settings for Sonatype
homepage := Some(url("https://github.com/slavaschmidt/ton-sdk-client-scala-binding"))
scmInfo := Some(ScmInfo(url("https://github.com/slavaschmidt/ton-sdk-client-scala-binding"), "git@github.com:slavaschmidt/ton-sdk-client-scala-binding.git"))
developers := List(Developer("slasch", "Slava Schmidt", "slavaschmidt@gmx.de", url("https://github.com/slavaschmidt")))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle := true

// Add sonatype repository settings
publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

envVars in Test := Map(
  "LD_LIBRARY_PATH" -> (resourceDirectory in Compile).value.getAbsolutePath,
  "PATH"            -> (resourceDirectory in Compile).value.getAbsolutePath
)

// To test against all versions: `> + test`
// To cross-publish: `> ; +publishSigned; sonatypeRelease`
