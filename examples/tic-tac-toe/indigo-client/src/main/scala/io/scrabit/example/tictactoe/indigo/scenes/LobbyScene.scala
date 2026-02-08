package io.scrabit.example.tictactoe.indigo.scenes

import indigo.*
import indigo.scenes.*

import io.scrabit.example.tictactoe.indigo.model.*
import io.scrabit.example.tictactoe.indigo.network.*

import generated.*

object LobbyScene extends Scene[Unit, Model, ViewModel]:

  val name: SceneName = SceneName("Lobby")

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
    case KeyboardEvent.KeyDown(key) if key == Key.BACKSPACE =>
      Outcome(model.copy(lobby = model.lobby.copy(newRoomName = model.lobby.newRoomName.dropRight(1))))

    case KeyboardEvent.KeyDown(key) if key == Key.ENTER =>
      if model.lobby.newRoomName.nonEmpty then
        model.connection.sessionKey match
          case Some(sk) =>
            Outcome(model)
              .addGlobalEvents(SendWs(Protocol.mkCreateRoom(sk, model.lobby.newRoomName)))
          case None =>
            Outcome(model)
      else Outcome(model)

    case e: KeyboardEvent.KeyDown if e.key.isPrintable =>
      val ch = if e.isShiftKeyDown then e.key.key.toUpperCase else e.key.key
      Outcome(model.copy(lobby = model.lobby.copy(newRoomName = model.lobby.newRoomName + ch)))

    case MouseEvent.Click(pt) =>
      val roomIndex = getRoomIndexAt(pt, model.lobby.rooms)
      roomIndex match
        case Some(room) =>
          model.connection.sessionKey match
            case Some(sk) =>
              Outcome(model)
                .addGlobalEvents(SendWs(Protocol.mkJoinRoom(sk, room.id)))
            case None =>
              Outcome(model)
        case None =>
          Outcome(model)

    case _ =>
      Outcome(model)

  private def getRoomIndexAt(pt: Point, rooms: List[Room]): Option[Room] =
    val startY = 200
    val roomHeight = 40
    val gap = 8
    rooms.zipWithIndex.find { case (_, i) =>
      val y = startY + i * (roomHeight + gap)
      pt.x >= 100 && pt.x <= 700 && pt.y >= y && pt.y <= y + roomHeight
    }.map(_._1)

  def updateViewModel(
      context: SceneContext[Unit],
      model: Model,
      sceneViewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
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

    val cyan  = RGBA.fromHexString("0x00d2ff")
    val coral = RGBA.fromHexString("0xff6b6b")
    val gold  = RGBA.fromHexString("0xffd93d")
    val panel = RGBA.fromHexString("0x16213e")
    val green = RGBA.fromHexString("0x00ff88")

    val header = Text(
      "LOBBY",
      340,
      30,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(cyan)
    )

    val welcome = Text(
      s"Welcome, ${model.connection.username}",
      100,
      70,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(green)
    )

    val roomsLabel = Text(
      "ROOMS",
      100,
      160,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val roomNodes: Batch[SceneNode] =
      if model.lobby.rooms.isEmpty then
        Batch(
          Text(
            "No rooms yet. Create one!",
            100,
            200,
            GameFont.fontKey,
            Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.5))
          )
        )
      else
        Batch.fromList(
          model.lobby.rooms.zipWithIndex.flatMap { case (room, i) =>
            val y = 200 + i * 48
            List(
              Shape.Box(
                Rectangle(100, y, 600, 40),
                Fill.Color(panel),
                Stroke(1, cyan.withAlpha(0.4))
              ),
              Text(
                s"${room.name}  (host: ${room.owner})",
                110,
                y + 12,
                GameFont.fontKey,
                Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White)
              ),
              Text(
                "JOIN",
                640,
                y + 12,
                GameFont.fontKey,
                Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
              )
            )
          }
        )

    val createY = math.max(200 + model.lobby.rooms.size * 48 + 30, 400)

    val createLabel = Text(
      "CREATE ROOM",
      100,
      createY,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(gold)
    )

    val createBox = Shape.Box(
      Rectangle(100, createY + 24, 400, 30),
      Fill.Color(panel),
      Stroke(2, cyan)
    )

    val createText = Text(
      model.lobby.newRoomName + (if sceneViewModel.cursorVisible then "_" else ""),
      106,
      createY + 32,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White)
    )

    val hint = Text(
      "Type name + ENTER to create",
      100,
      createY + 64,
      GameFont.fontKey,
      Assets.assets.generated.GameFontMaterial.toImageEffects.withTint(RGBA.White.withAlpha(0.4))
    )

    Outcome(
      SceneUpdateFragment(
        Batch(header, welcome, roomsLabel, createLabel, createBox, createText, hint) ++ roomNodes
      )
    )
