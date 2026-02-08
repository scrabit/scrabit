package io.scrabit.example.tictactoe.indigo.scenes

import indigo.*
import indigo.scenes.*

import io.scrabit.example.tictactoe.indigo.model.*
import io.scrabit.example.tictactoe.indigo.network.*

import generated.*

object GameScene extends Scene[Unit, Model, ViewModel]:

  val name: SceneName = SceneName("Game")

  type SceneModel     = Model
  type SceneViewModel = ViewModel

  val modelLens: Lens[Model, Model] =
    Lens.keepLatest

  val viewModelLens: Lens[ViewModel, ViewModel] =
    Lens.keepLatest

  val eventFilters: EventFilters = EventFilters.Permissive

  val subSystems: Set[SubSystem[Model]] = Set()

  private val boardOriginX = 200
  private val boardOriginY = 140

  private def cellSize(boardSize: Int): Int =
    math.min(400 / boardSize, 120)

  def updateModel(
      context: SceneContext[Unit],
      model: Model
  ): GlobalEvent => Outcome[Model] =
    case KeyboardEvent.KeyDown(key) if key == Key.SPACE =>
      model.game match
        case Some(g) if !g.started =>
          model.connection.sessionKey match
            case Some(sk) =>
              Outcome(model).addGlobalEvents(SendWs(Protocol.mkReady(sk)))
            case None =>
              Outcome(model)
        case _ =>
          Outcome(model)

    case KeyboardEvent.KeyDown(key) if key == Key.ESCAPE =>
      Outcome(model.leaveGame)
        .addGlobalEvents(SceneEvent.JumpTo(LobbyScene.name))

    case MouseEvent.Click(pt) =>
      model.game match
        case Some(g) if g.started && g.isMyTurn && g.winner.isEmpty =>
          val cs = cellSize(g.board.size)
          val col = (pt.x - boardOriginX) / cs
          val row = (pt.y - boardOriginY) / cs
          if col >= 0 && col < g.board.size && row >= 0 && row < g.board.size then
            g.board.get(col, row) match
              case Some(Cell(None)) =>
                model.connection.sessionKey match
                  case Some(sk) =>
                    Outcome(model).addGlobalEvents(SendWs(Protocol.mkMove(sk, col, row)))
                  case None =>
                    Outcome(model)
              case _ => Outcome(model)
          else Outcome(model)
        case _ => Outcome(model)

    case _ =>
      Outcome(model)

  def updateViewModel(
      context: SceneContext[Unit],
      model: Model,
      sceneViewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
    case MouseEvent.Move(pt) =>
      model.game match
        case Some(g) if g.started =>
          val cs = cellSize(g.board.size)
          val col = (pt.x - boardOriginX) / cs
          val row = (pt.y - boardOriginY) / cs
          if col >= 0 && col < g.board.size && row >= 0 && row < g.board.size then
            Outcome(sceneViewModel.copy(hoverCell = Some((col, row))))
          else
            Outcome(sceneViewModel.copy(hoverCell = None))
        case _ =>
          Outcome(sceneViewModel.copy(hoverCell = None))

    case FrameTick =>
      val elapsed = context.frame.time.running.toDouble
      val shouldBlink = (elapsed - sceneViewModel.lastBlink) > 0.5
      if shouldBlink then
        Outcome(sceneViewModel.copy(cursorVisible = !sceneViewModel.cursorVisible, lastBlink = elapsed))
      else
        Outcome(sceneViewModel)

    case _ =>
      Outcome(sceneViewModel)

  def present(
      context: SceneContext[Unit],
      model: Model,
      sceneViewModel: ViewModel
  ): Outcome[SceneUpdateFragment] =
    model.game match
      case None =>
        Outcome(SceneUpdateFragment.empty)
      case Some(g) =>
        if g.started then presentBoard(g, sceneViewModel)
        else presentWaiting(g, model.connection.username)

  private val cyan  = RGBA.fromHexString("0x00d2ff")
  private val coral = RGBA.fromHexString("0xff6b6b")
  private val gold  = RGBA.fromHexString("0xffd93d")
  private val panel = RGBA.fromHexString("0x16213e")
  private val green = RGBA.fromHexString("0x00ff88")

  private def presentWaiting(g: GameModel, username: String): Outcome[SceneUpdateFragment] =
    val header = Text(
      s"ROOM: ${g.roomName}",
      200,
      40,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(cyan)
    )

    val playersLabel = Text(
      "PLAYERS",
      200,
      100,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val playerNodes: Batch[SceneNode] = Batch.fromList(
      g.players.zipWithIndex.flatMap { case (p, i) =>
        val y = 130 + i * 36
        val readyText = if p.isReady then "[READY]" else "[NOT READY]"
        val color = if p.isReady then green else coral
        List(
          Text(
            s"${p.userId} $readyText",
            220,
            y,
            GameFont.fontKey,
            Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(color)
          )
        )
      }
    )

    val hint = Text(
      "SPACE: ready  ESC: leave",
      200,
      400,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.4))
    )

    Outcome(
      SceneUpdateFragment(
        Batch(header, playersLabel, hint) ++ playerNodes
      )
    )

  private def presentBoard(g: GameModel, vm: ViewModel): Outcome[SceneUpdateFragment] =
    val cs = cellSize(g.board.size)

    val turnText =
      if g.isMyTurn then "YOUR TURN"
      else "WAITING..."

    val turnColor = if g.isMyTurn then green else RGBA.White.withAlpha(0.6)

    val turnIndicator = Text(
      turnText,
      boardOriginX,
      40,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(turnColor)
    )

    val myMarkText = g.myMark match
      case Some(Mark.X) => "You are: X"
      case Some(Mark.O) => "You are: O"
      case None         => ""

    val markLabel = Text(
      myMarkText,
      boardOriginX,
      70,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val roomLabel = Text(
      s"Room: ${g.roomName}",
      boardOriginX,
      100,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.5))
    )

    val boardNodes: Batch[SceneNode] = Batch.fromIndexedSeq(
      for
        row <- (0 until g.board.size)
        col <- (0 until g.board.size)
      yield
        val x = boardOriginX + col * cs
        val y = boardOriginY + row * cs
        val cell = g.board.get(col, row).getOrElse(Cell(None))
        val isHover = vm.hoverCell.contains((col, row)) && g.isMyTurn && cell.mark.isEmpty

        val bgColor =
          if isHover then panel.withAlpha(0.8)
          else panel.withAlpha(0.4)

        val borderColor = cell.mark match
          case Some(Mark.X) => cyan
          case Some(Mark.O) => coral
          case None         => RGBA.White.withAlpha(0.2)

        val box = Shape.Box(
          Rectangle(x, y, cs - 2, cs - 2),
          Fill.Color(bgColor),
          Stroke(2, borderColor)
        )

        box
    )

    val markNodes: Batch[SceneNode] = Batch.fromIndexedSeq(
      for
        row <- (0 until g.board.size)
        col <- (0 until g.board.size)
        cell = g.board.get(col, row).getOrElse(Cell(None))
        if cell.mark.isDefined
      yield
        val x = boardOriginX + col * cs + cs / 2 - 8
        val y = boardOriginY + row * cs + cs / 2 - 8
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
        )
    )

    val hint = Text(
      "Click cell to move  ESC: leave",
      boardOriginX,
      boardOriginY + g.board.size * cs + 20,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.4))
    )

    Outcome(
      SceneUpdateFragment(
        Batch(turnIndicator, markLabel, roomLabel, hint) ++ boardNodes ++ markNodes
      )
    )
