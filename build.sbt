lazy val scala3Version = "3.2.2"
lazy val scala2Version = "2.13.10"
lazy val supportedScalaVersions = List(scala2Version, scala3Version)

lazy val zioVersion = "2.0.10"
lazy val circeVersion = "0.14.5"
lazy val ampsVersion = "5.3.3.4"

ThisBuild / scalaVersion := scala3Version

lazy val core = (project in file("core"))
  .settings(
    name := "zio-amps",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      "com.crankuptheamps" % "amps-client" % ampsVersion,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )
  )

lazy val processing = (project in file("processing"))
  .settings(
    name := "zio-amps-processing",
    crossScalaVersions := supportedScalaVersions
  )
  .dependsOn(core)

lazy val zioAmpsExamples = (project in file("examples"))
  .settings(
    name := "zio-amps-examples",
    publish / skip := true,
    Global / cancelable := true,
    Global / connectInput := true,
    Global / fork := true
  )
  .dependsOn(core, processing)

lazy val root = (project in file("."))
  .aggregate(core, zioAmpsExamples)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )
