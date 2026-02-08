package io.scrabit.example.tictactoe.indigo.scenes

import indigo.*
import indigo.scenes.*

import io.scrabit.example.tictactoe.indigo.model.*
import io.scrabit.example.tictactoe.indigo.network.*

import generated.*

object GameOverScene extends Scene[Unit, Model, ViewModel]:

  val name: SceneName = SceneName("GameOver")

  type SceneModel     = Model
  type SceneViewModel = ViewModel

  val modelLens: Lens[Model, Model] =
    Lens.keepLatest

  val viewModelLens: Lens[ViewModel, ViewModel] =
    Lens.keepLatest

  val eventFilters: EventFilters = EventFilters.Permissive

  val subSystems: Set[SubSystem[Model]] = Set()

  def updateModel(
      context: SceneContext[Unit],
      model: Model
  ): GlobalEvent => Outcome[Model] =
    case KeyboardEvent.KeyDown(key) if key == Key.ENTER =>
      Outcome(model.leaveGame)
        .addGlobalEvents(SceneEvent.JumpTo(LobbyScene.name))

    case MouseEvent.Click(pt) =>
      if pt.x >= 250 && pt.x <= 550 && pt.y >= 440 && pt.y <= 476 then
        Outcome(model.leaveGame)
          .addGlobalEvents(SceneEvent.JumpTo(LobbyScene.name))
      else
        Outcome(model)

    case _ =>
      Outcome(model)

  def updateViewModel(
      context: SceneContext[Unit],
      model: Model,
      sceneViewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
    _ => Outcome(sceneViewModel)

  def present(
      context: SceneContext[Unit],
      model: Model,
      sceneViewModel: ViewModel
  ): Outcome[SceneUpdateFragment] =

    val cyan  = RGBA.fromHexString("0x00d2ff")
    val coral = RGBA.fromHexString("0xff6b6b")
    val gold  = RGBA.fromHexString("0xffd93d")
    val panel = RGBA.fromHexString("0x16213e")
    val green = RGBA.fromHexString("0x00ff88")

    val centerX = 400

    val header = Text(
      "GAME OVER",
      centerX - 70,
      60,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val (resultText, resultColor) = model.game match
      case Some(g) =>
        g.winner match
          case Some(Some(name)) if name == model.connection.username =>
            ("YOU WIN!", green)
          case Some(Some(name)) =>
            (s"$name WINS!", coral)
          case Some(None) =>
            ("DRAW!", RGBA.White)
          case None =>
            ("GAME ENDED", RGBA.White)
      case None =>
        ("GAME ENDED", RGBA.White)

    val result = Text(
      resultText,
      centerX - 60,
      130,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(resultColor)
    )

    val boardNodes: Batch[SceneNode] = model.game match
      case Some(g) =>
        val cs = math.min(300 / g.board.size, 80)
        val ox = centerX - (g.board.size * cs) / 2
        val oy = 190

        val cells = Batch.fromIndexedSeq(
          for
            row <- (0 until g.board.size)
            col <- (0 until g.board.size)
          yield
            val x = ox + col * cs
            val y = oy + row * cs
            val cell = g.board.get(col, row).getOrElse(Cell(None))
            val borderColor = cell.mark match
              case Some(Mark.X) => cyan
              case Some(Mark.O) => coral
              case None         => RGBA.White.withAlpha(0.2)

            Shape.Box(
              Rectangle(x, y, cs - 2, cs - 2),
              Fill.Color(panel.withAlpha(0.4)),
              Stroke(2, borderColor)
            ): SceneNode
        )

        val marks = Batch.fromIndexedSeq(
          for
            row <- (0 until g.board.size)
            col <- (0 until g.board.size)
            cell = g.board.get(col, row).getOrElse(Cell(None))
            if cell.mark.isDefined
          yield
            val x = ox + col * cs + cs / 2 - 8
            val y = oy + row * cs + cs / 2 - 8
            val (markStr, color) = cell.mark match
              case Some(Mark.X) => ("X", cyan)
              case Some(Mark.O) => ("O", coral)
              case None         => ("", RGBA.White)

            Text(
              markStr,
              x,
              y,
              GameFont.fontKey,
              Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(color)
            ): SceneNode
        )

        cells ++ marks

      case None =>
        Batch.empty

    val backButton = Shape.Box(
      Rectangle(250, 440, 300, 36),
      Fill.Color(cyan.withAlpha(0.2)),
      Stroke(2, cyan)
    )

    val backText = Text(
      "BACK TO LOBBY",
      centerX - 90,
      450,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(cyan)
    )

    val hint = Text(
      "ENTER or click to continue",
      centerX - 160,
      500,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.4))
    )

    Outcome(
      SceneUpdateFragment(
        Batch(header, result, backButton, backText, hint) ++ boardNodes
      )
    )
