package io.scrabit.example.tictactoe

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.websocket.*
import scala.scalajs.js.annotation.*
import io.circe.Json
import io.circe.syntax.*
import io.scrabit.example.tictactoe.model.*
import io.scrabit.example.tictactoe.model.Msg.*
import io.scrabit.example.tictactoe.model.Request.*
import io.scrabit.example.tictactoe.view.Menu


@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, State]:

  def main(args: Array[String]): Unit = launch("app") // mount the app to div with id="app"

  def router: Location => Msg = Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (State, Cmd[IO, Msg]) =
    (State.init, Cmd.None)

  def update(model: State): Msg => (State, Cmd[IO, Msg]) =
    case Msg.WebSocketStatus(status) =>
      val (nextWS, cmds) = model.echoSocket.update(status)
      (model.copy(echoSocket = nextWS), cmds)

    case Msg.FromSocket(message) =>
      val logWS = Logger.info[IO]("Got: " + message)
      val msg = ServerMessage.parse(message) match
        case None =>
          Msg.NoOp
        case Some(ServerMessage.LoginSuccess(sessionKey)) =>
          Msg.UpdateSessionKey(sessionKey)

        case Some(ServerMessage.LoginFailed(msg)) =>
          Msg.NoOp

        case Some(ServerMessage.LobbyInfo(rooms)) =>
          Msg.UpdateLobby(rooms)

        case Some(ServerMessage.JoinedRoom(id)) =>
          Msg.JoinRoom(id, "changeme")

        case Some(ServerMessage.ReadySync(players)) =>
          Msg.UpdateReadyState(players)

        case Some(ServerMessage.BoardSync(currentTurn, board)) =>
          Msg.UpdateBoard(currentTurn, board)

        case Some(ServerMessage.GameResult(winner)) =>
          Msg.GameResult(winner)

      val cmds = Cmd.Batch(logWS, Cmd.Emit(msg))
      (model.copy(log = message :: model.log), cmds)

    case Msg.Login =>
      val username = model.loginState.username
      val password = model.loginState.password
      (model, model.echoSocket.publish(s"LOGIN-$username/$password"))

    case Msg.CreateRoom(name) =>
      (model, Cmd.Emit(Msg.SendRequest(CreateRoomRequest(name).asJsonObject.asJson.noSpaces)))

    case Msg.UpdateLoginState(newState) =>
      (model.copy(loginState = newState),Cmd.None)

    case Msg.UpdateLobby(rooms) =>
      (model.updateRoomList(rooms), Cmd.None)

    case Msg.JoinRoom(id, name) =>
      (model.joinedRoom(id, name), Cmd.None)

    case Msg.UpdateReadyState(players) =>
      (model.updateReadyState(players), Cmd.None)

    case Msg.UpdateBoard(currentTurn, board) =>
      (model.updateBoard(currentTurn, board), Cmd.None)

    case Msg.GameResult(winner) =>
      (model.gameEnded(winner), Cmd.None)

    case Msg.SendRequest(request) =>
      model.lobby.map(_.sessionKey) match
        case None =>
          (model, Logger.error("no session key"))
        case Some(key) =>
          // Parse the request string back to JSON and add session key
          io.circe.parser.parse(request) match
            case Left(_) =>
              (model, Logger.error("Invalid request JSON"))
            case Right(json) =>
              val requestWithSessionKey = json.asObject.map(_.add("sessionKey", key.asJson))
                .map(_.asJson.noSpaces)
                .getOrElse(request)
              (model, model.echoSocket.publish(requestWithSessionKey))

    case Msg.UpdateSessionKey(key) =>
      (model.updateSessionKey(key), Cmd.None)

    case Msg.NoOp =>
      (model, Cmd.None)

  def view(state: State): Html[Msg] =
    div(cls :="center")(Menu(state))

  def subscriptions(state: State): Sub[IO, Msg] =
    state.echoSocket.subscribe {
      case WebSocketEvent.Error(errorMesage) =>
        Msg.FromSocket(errorMesage)

      case WebSocketEvent.Receive(message) =>
        Msg.FromSocket(message)

      case WebSocketEvent.Open =>
        Msg.FromSocket("Connected to game server")

      case WebSocketEvent.Close(code, reason) =>
        Msg.FromSocket(s"<socket closed> - code: $code, reason: $reason")

      case WebSocketEvent.Heartbeat =>
        Msg.NoOp
    }

