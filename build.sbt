ThisBuild / scalaVersion     := "3.3.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.scrabit"
ThisBuild / organizationName := "scrabit"

val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"
val CirceVersion = "0.14.6"
val LogbackVersion = "1.4.5"
val ScalaTestVersion = "3.2.18"
val RefinedVersion = "0.11.1"

lazy val root = (project in file("."))
  .settings(
    name := "game-server",
    libraryDependencies ++= Seq(
      "eu.timepit" %% "refined" % RefinedVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http"% PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-cors" % PekkoHttpVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "ch.qos.logback" % "logback-core" % LogbackVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    )
  )
