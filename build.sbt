ThisBuild / scalaVersion     := "3.8.1"
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

val IndigoVersion = "0.22.0"

lazy val ticTacToeIndigoClient = (project in file("examples/tic-tac-toe/indigo-client"))
  .enablePlugins(ScalaJSPlugin, indigoplugin.SbtIndigo)
  .settings(commonSettings)
  .settings(
    name := "tic-tac-toe-indigo-client",
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "indigo"            % IndigoVersion,
      "io.indigoengine" %%% "indigo-extras"     % IndigoVersion,
      "io.indigoengine" %%% "indigo-json-circe" % IndigoVersion,
      "io.circe"        %%% "circe-core"        % CirceVersion,
      "io.circe"        %%% "circe-generic"     % CirceVersion,
      "io.circe"        %%% "circe-parser"      % CirceVersion
    ),
    Compile / sourceGenerators += Def.task {
      val cacheDir     = (Compile / sourceManaged).value / "indigo-generated"
      val base         = os.Path(baseDirectory.value.getCanonicalPath)
      val assetsDir    = base / "assets"
      val opts = indigoplugin.IndigoOptions.defaults
        .withTitle("Tic Tac Toe")
        .withWindowSize(800, 600)
        .withBackgroundColor("0x1a1a2e")
        .withAssetDirectory(os.RelPath.rel / "assets")
      val assets = opts.assets.copy(
        gameAssetsDirectory = os.RelPath.rel / "assets"
      )
      val generators = indigoplugin.IndigoGenerators("generated")
        .generateConfig("Config", opts)
        .embedFont(
          "GameFont",
          base / "generator-data" / "PressStart2P.ttf",
          indigoplugin.FontOptions(
            "GameFont",
            16,
            indigoplugin.CharSet.ASCII,
            indigoplugin.RGB.White,
            false,
            indigoplugin.FontLayout.default
          ),
          assetsDir / "generated"
        )
        .listAssets("Assets",
          indigoplugin.IndigoAssets.defaults
            .withAssetDirectory(os.RelPath.rel / "examples" / "tic-tac-toe" / "indigo-client" / "assets")
            .withExclude {
              case p if p.last.startsWith(".") => true
              case _ => false
            }
        )
      generators.toSourceFiles(cacheDir)
    },
    indigoplugin.SbtIndigo.autoImport.indigoOptions :=
      indigoplugin.IndigoOptions.defaults
        .withTitle("Tic Tac Toe")
        .withWindowSize(800, 600)
        .withBackgroundColor("0x1a1a2e")
        .withAssetDirectory(os.RelPath.rel / "examples" / "tic-tac-toe" / "indigo-client" / "assets")
  )
  .dependsOn(ticTacToecCommon)

lazy val ticTacToeTyrianClient =
  (project in file("examples/tic-tac-toe/tyrian-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "io.indigoengine" %%% "tyrian-io"     % "0.14.0",
        "io.circe"        %%% "circe-core"    % CirceVersion,
        "io.circe"        %%% "circe-generic" % CirceVersion,
        "io.circe"        %%% "circe-parser"  % CirceVersion
      ),
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
      autoAPIMappings := true,
      jsTool := JSToolConfig(
        installPackagesCommand = Cmd("npm install --prefix examples/tic-tac-toe/tyrian-client", ".*".r),
        dev = DevConfig(
          command = Cmd("npm run --prefix examples/tic-tac-toe/tyrian-client dev", ".*ready.*".r),
          startupMessage = "Starting dev server...",
          successMessage = "Dev server ready!"
        ),
        build = BuildConfig(
          command = Cmd("npm run --prefix examples/tic-tac-toe/tyrian-client build", ".*built.*".r),
          startupMessage = "Building for production...",
          successMessage = "Build complete!"
        )
      )
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
  scalaVersion := "3.8.1"
)
