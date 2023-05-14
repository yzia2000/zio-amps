lazy val scala3Version = "3.2.2"

lazy val zioVersion = "2.0.12"
lazy val circeVersion = "0.14.5"
lazy val ampsVersion = "5.3.3.4"

ThisBuild / scalaVersion := scala3Version

Global / cancelable := true
Global / connectInput := true
Global / fork := true

lazy val core = (project in file("core"))
  .settings(
    name := "zio-amps",
    libraryDependencies ++= Seq(
      "com.crankuptheamps" % "amps-client" % ampsVersion,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion
    )
  )

lazy val processing = (project in file("processing"))
  .settings(
    name := "zio-amps-processing"
  )
  .dependsOn(core)

lazy val zioAmpsExamples = (project in file("examples"))
  .settings(
    name := "zio-amps-examples",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % "0.5.0"
    )
  )
  .dependsOn(processing)

lazy val root = (project in file("."))
  .aggregate(core, zioAmpsExamples)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )
