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
import io.scrabit.example.tictactoe.view.{Menu, LobbyView, GameView}


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

        case Some(ServerMessage.JoinedRoom(roomId, userId)) =>
          if (userId == model.loginState.username) {
            Msg.JoinRoom(roomId, "Game Room")
          } else {
            Msg.UserJoinedRoom(roomId, userId)
          }

        case Some(ServerMessage.ReadySync(players)) =>
          Msg.UpdateReadyState(players)

        case Some(ServerMessage.BoardSync(currentTurn, board)) =>
          Msg.UpdateBoard(currentTurn, board)

        case Some(ServerMessage.GameResult(winner)) =>
          Msg.GameResult(winner)

        case Some(ServerMessage.RoomCreated(roomId, roomName)) =>
          Msg.JoinRoom(roomId, roomName)

      val cmds = Cmd.Batch(logWS, Cmd.Emit(msg))
      (model.copy(log = message :: model.log), cmds)

    case Msg.Login =>
      val username = model.loginState.username
      val password = model.loginState.password
      (model, model.echoSocket.publish(s"LOGIN-$username/$password"))

    case Msg.CreateRoom(name) =>
      (model, Cmd.Emit(Msg.SendRequest(CreateRoomRequest(name).asJsonObject.asJson.noSpaces)))

    case Msg.JoinSpecificRoom(roomId) =>
      (model, Cmd.Emit(Msg.SendRequest(JoinRoomRequest(roomId).asJsonObject.asJson.noSpaces)))

    case Msg.ToggleReady =>
      (model, Cmd.Emit(Msg.SendRequest(ReadyRequest().asJsonObject.asJson.noSpaces)))

    case Msg.MakeMove(x, y) =>
      if (model.game.exists(_.isMyTurn)) {
        (model, Cmd.Emit(Msg.SendRequest(MoveRequest(x, y).asJsonObject.asJson.noSpaces)))
      } else {
        (model, Logger.info("Not your turn!"))
      }

    case Msg.BackToLobby =>
      // Reset game state and stay in lobby
      (model.copy(game = None), Cmd.None)

    case Msg.UpdateLoginState(newState) =>
      (model.copy(loginState = newState), Cmd.None)

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

    case Msg.UpdateNewRoomName(name) =>
      val updatedLobby = model.lobby.map(_.copy(newRoomName = name))
      (model.copy(lobby = updatedLobby), Cmd.None)

    case Msg.UserJoinedRoom(roomId, userId) =>
      model.game match {
        case Some(game) if game.roomId == roomId =>
          // Add the new user to the current game's player list
          val newPlayer = State.Player(userId, isReady = false)
          val existingPlayerIds = game.players.map(_.userId).toSet
          val updatedPlayers = if (existingPlayerIds.contains(userId)) {
            game.players // User already in list, don't duplicate
          } else {
            game.players :+ newPlayer
          }
          val updatedGame = game.copy(players = updatedPlayers)
          (model.copy(game = Some(updatedGame)), Logger.info(s"User $userId joined room $roomId"))
        case _ =>
          // Not in the same room or not in any game
          (model, Logger.info(s"User $userId joined room $roomId (different room)"))
      }

    case Msg.RestartGame =>
      model.game match {
        case Some(game) =>
          // Reset the game board and winner, but keep players and room info
          val resetGame = game.copy(board = State.Board.empty, winner = None, isMyTurn = false)
          (model.copy(game = Some(resetGame)), Logger.info("Game restarted"))
        case None =>
          (model, Cmd.None)
      }

    case Msg.NoOp =>
      (model, Cmd.None)

  def view(state: State): Html[Msg] =
    div(cls := "center")(
      if (!state.loginState.isLoggedIn) {
        // Show login screen
        Menu(state)
      } else if (state.game.isEmpty) {
        // Show lobby
        LobbyView(state)
      } else {
        // Show game
        GameView(state)
      }
    )

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

