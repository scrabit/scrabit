ThisBuild / scalaVersion     := "3.6.0"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.scrabit"
ThisBuild / organizationName := "scrabit"

val PekkoVersion      = "1.1.2"
val PekkoHttpVersion  = "1.1.0"
val CirceVersion      = "0.14.10"
val LogbackVersion    = "1.4.5"
val ScalaTestVersion  = "3.2.19"
val IronVersion       = "2.6.0"
val ConsoleboxVersion = "0.2.1"

lazy val gameServer = (project in file("game-server"))
  .settings(commonSettings)
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
      "com.w47s0n"         %% "consolebox"                % ConsoleboxVersion,
      "org.apache.pekko"   %% "pekko-actor-testkit-typed" % PekkoVersion     % Test,
      "org.scalatest"      %% "scalatest"                 % ScalaTestVersion % Test
    )
  )

lazy val ticTacToeServer =
  (project in file("examples/tic-tac-toe/server"))
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % ScalaTestVersion % Test)
    )
    .dependsOn(gameServer, ticTacToecCommon)

//TODO: add Indigo client
lazy val ticTacToeIndigoClient = (project in file("examples/tic-tac-toe/indigo-client"))
  .dependsOn(ticTacToecCommon)

lazy val ticTacToeTyrianClient =
  (project in file("examples/tic-tac-toe/tyrian-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "io.indigoengine" %%% "tyrian-io"     % "0.11.0",
        "io.circe"        %%% "circe-core"    % CirceVersion,
        "io.circe"        %%% "circe-generic" % CirceVersion,
        "io.circe"        %%% "circe-parser"  % CirceVersion
      ),
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
      autoAPIMappings := true
    )

lazy val ticTacToecCommon = (project in file("examples/tic-tac-toe/common"))
  .settings(commonSettings)

lazy val coinFlipUltimate = (project in file("examples/coin-flip-ultimate"))
  .settings(commonSettings)
  .settings(
    name := "coin-flip-ultimate"
  )
  .dependsOn(gameServer)

lazy val commonSettings = Seq(
  scalaVersion := "3.7.1"
)
