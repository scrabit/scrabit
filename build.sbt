ThisBuild / scalaVersion     := "3.6.0"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.scrabit"
ThisBuild / organizationName := "scrabit"

val PekkoVersion     = "1.1.2"
val PekkoHttpVersion = "1.1.0"
val CirceVersion     = "0.14.10"
val LogbackVersion   = "1.4.5"
val ScalaTestVersion = "3.2.19"
val IronVersion      = "2.6.0"

lazy val gameServer = (project in file("game-server"))
  .settings(
    name := "game-server",
    libraryDependencies ++= Seq(
      "io.github.iltotore" %% "iron"                      % IronVersion,
      "io.circe"           %% "circe-core"                % CirceVersion,
      "io.circe"           %% "circe-generic"             % CirceVersion,
      "io.circe"           %% "circe-parser"              % CirceVersion,
      "org.apache.pekko"   %% "pekko-actor-typed"         % PekkoVersion,
      "org.apache.pekko"   %% "pekko-stream-typed"        % PekkoVersion,
      "org.apache.pekko"   %% "pekko-http"                % PekkoHttpVersion,
      "org.apache.pekko"   %% "pekko-http-cors"           % PekkoHttpVersion,
      "ch.qos.logback"      % "logback-classic"           % LogbackVersion,
      "ch.qos.logback"      % "logback-core"              % LogbackVersion,
      "org.apache.pekko"   %% "pekko-actor-testkit-typed" % PekkoVersion     % Test,
      "org.scalatest"      %% "scalatest"                 % ScalaTestVersion % Test
    )
  )

lazy val ticTacToeServer =
  (project in file("examples/tic-tac-toe/server"))
    .settings(
      libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % ScalaTestVersion % Test)
    )
    .dependsOn(gameServer, ticTacToecCommon)

lazy val ticTacToeClient = (project in file("examples/tic-tac-toe/client"))
  .dependsOn(gameServer, ticTacToecCommon)

lazy val ticTacToecCommon = (project in file("examples/tic-tac-toe/common"))

lazy val coinFlipUltimate = (project in file("examples/coin-flip-ultimate"))
  .settings(
    name := "coin-flip-ultimate"
  )
  .dependsOn(gameServer)
