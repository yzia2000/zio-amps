lazy val scala3Version = "3.2.2"
lazy val supportedScalaVersions = List(scala3Version)

lazy val zioVersion = "2.0.10"
lazy val circeVersion = "0.14.5"
lazy val ampsVersion = "5.3.3.4"

lazy val core = (project in file("core"))
  .settings(
    name := "zio-amps",
    scalaVersion := "3.2.2",
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

lazy val zioAmpsExamples = (project in file("examples"))
  .settings(
    scalaVersion := scala3Version,
    name := "zio-amps-examples",
    crossScalaVersions := supportedScalaVersions,
    publish / skip := true,
    Global / cancelable := true,
    Global / connectInput := true,
    Global / fork := true
  )
  .dependsOn(core)

lazy val root = (project in file("."))
  .aggregate(core, zioAmpsExamples)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )
