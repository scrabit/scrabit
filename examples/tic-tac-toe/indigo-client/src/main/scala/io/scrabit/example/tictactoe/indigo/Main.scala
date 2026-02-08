package io.scrabit.example.tictactoe.indigo

import indigo.*
import indigo.scenes.*
import indigo.shared.networking.{WebSocketEvent, WebSocketConfig, WebSocketId}

import io.scrabit.example.tictactoe.indigo.model.*
import io.scrabit.example.tictactoe.indigo.network.*
import io.scrabit.example.tictactoe.indigo.scenes.*

import generated.*

import scala.scalajs.js.annotation.*

@JSExportTopLevel("IndigoGame")
object Main extends IndigoGame[Unit, Unit, Model, ViewModel]:

  def scenes(bootData: Unit): NonEmptyBatch[Scene[Unit, Model, ViewModel]] =
    NonEmptyBatch(LoginScene, LobbyScene, GameScene, GameOverScene)

  def initialScene(bootData: Unit): Option[SceneName] =
    Some(LoginScene.name)

  def eventFilters: EventFilters =
    EventFilters.Permissive

  def boot(flags: Map[String, String]): Outcome[BootResult[Unit, Model]] =
    Outcome(
      BootResult(
        Config.config.noResize
          .withClearColor(RGBA.fromHexString("0x1a1a2e")),
        ()
      )
        .withFonts(GameFont.fontInfo)
        .withAssets(Assets.assets.generated.assetSet)
    )

  def setup(
      bootData: Unit,
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[Unit]] =
    Outcome(Startup.Success(()))

  def initialModel(startupData: Unit): Outcome[Model] =
    Outcome(Model.initial)

  def initialViewModel(startupData: Unit, model: Model): Outcome[ViewModel] =
    Outcome(ViewModel.initial)

  def updateModel(context: Context[Unit], model: Model): GlobalEvent => Outcome[Model] =
    case WebSocketEvent.Receive(_, raw) =>
      Protocol.parse(raw) match
        case Some(Protocol.LoginSuccess(key)) =>
          Outcome(model.withSession(key))
            .addGlobalEvents(SceneEvent.JumpTo(LobbyScene.name))

        case Some(Protocol.LoginFailed(err)) =>
          Outcome(model.withError(err))

        case Some(Protocol.LobbyInfo(rooms)) =>
          Outcome(model.withRooms(rooms))

        case Some(Protocol.RoomCreated(roomId, roomName)) =>
          Outcome(model.joinRoom(roomId, roomName))
            .addGlobalEvents(SceneEvent.JumpTo(GameScene.name))

        case Some(Protocol.JoinedRoom(roomId, userId)) =>
          val updated = model.game match
            case Some(g) if g.roomId == roomId => model
            case _ => model.joinRoom(roomId, s"room-$roomId")
          Outcome(updated)
            .addGlobalEvents(SceneEvent.JumpTo(GameScene.name))

        case Some(Protocol.ReadySync(players)) =>
          Outcome(model.withPlayers(players))

        case Some(Protocol.BoardSync(currentTurn, board)) =>
          Outcome(model.withBoard(currentTurn, board))

        case Some(Protocol.GameResult(winner)) =>
          Outcome(model.withResult(winner))
            .addGlobalEvents(SceneEvent.JumpTo(GameOverScene.name))

        case _ =>
          Outcome(model)

    case SendWs(data) =>
      Outcome(model)
        .addGlobalEvents(WebSocketEvent.Send(data, model.connection.wsConfig))

    case WebSocketEvent.Error(_, err) =>
      Outcome(model.withError(err))

    case WebSocketEvent.Close(_) =>
      Outcome(model.disconnected)

    case _ =>
      Outcome(model)

  def updateViewModel(
      context: Context[Unit],
      model: Model,
      viewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
    _ => Outcome(viewModel)

  def present(
      context: Context[Unit],
      model: Model,
      viewModel: ViewModel
  ): Outcome[SceneUpdateFragment] =
    Outcome(SceneUpdateFragment.empty)
