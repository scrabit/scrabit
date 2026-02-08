package io.scrabit.example.tictactoe.handler.connection

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.msg.lobby.LobbyMsg
import io.scrabit.example.tictactoe.msg.game.GameMsg
import io.scrabit.example.tictactoe.state.State
import tyrian.Cmd
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg
import io.scrabit.example.tictactoe.model.ServerMessage
import io.scrabit.example.tictactoe.state.ui.NotificationLevel
import cats.effect.IO
import tyrian.websocket.KeepAliveSettings
import tyrian.websocket.WebSocket
import tyrian.websocket.WebSocketConnect

object ConnectionHandler extends MessageHandler[IO, WebSocketMsg]:
  private val backendUrl = "ws://localhost:8080"

  override protected def handle(message: WebSocketMsg, currentState: State): Result =
    given State = currentState
    message match
      case WebSocketMsg.Connect =>
        currentState -> connect(backendUrl)
      case WebSocketMsg.Connecting(ws) =>
        updateConnectionStatus(ConnectionStatus.Connecting(ws)) -> logDebug(s"Connecting to $backendUrl")
      case WebSocketMsg.Error(e) =>
        updateConnectionStatus(ConnectionStatus.ConnectionError(e))
          .withNotification("Connection error", NotificationLevel.Error)
          .withLoading(false) -> Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Error, e)))
      case WebSocketMsg.Connected =>
        updateConnectionStatus(currentState.connection.toConnected)
          .withNotification("Connected to server", NotificationLevel.Success) -> logDebug("Connected")
      case WebSocketMsg.Disconnected =>
        updateConnectionStatus(ConnectionStatus.Disconnected) -> logDebug("Disconnected")
      case WebSocketMsg.ReceiveData(value) =>
        handleReceiveData(value, currentState)
      case WebSocketMsg.SendData(data) =>
        currentState.connection match
          case ConnectionStatus.Connected(ws) =>
            currentState -> sendData(ws, data)
          case _ =>
            currentState -> logDebug("Can't send data, websocket disconnected")

  private def handleReceiveData(value: String, state: State): Result =
    ServerMessage.parse(value) match
      case Some(ServerMessage.LoginSuccess(sessionKey)) =>
        state.withSession(sessionKey)
          .withLoading(false)
          .withNotification("Logged in!", NotificationLevel.Success) -> logDebug(s"Login success, session: $sessionKey")

      case Some(ServerMessage.LoginFailed(error)) =>
        state
          .withLoading(false)
          .withNotification(s"Login failed: $error", NotificationLevel.Error) -> Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Error, s"Login failed: $error")))

      case Some(ServerMessage.LobbyInfo(rooms)) =>
        state.withRooms(rooms) -> logDebug(s"Lobby info: ${rooms.size} rooms")

      case Some(ServerMessage.RoomCreated(roomId, roomName)) =>
        state.joinedRoom(roomId, roomName) -> logDebug(s"Room created: $roomName ($roomId)")

      case Some(ServerMessage.JoinedRoom(roomId, userId)) =>
        val roomName = state.lobby.flatMap(_.rooms.find(_.id == roomId).map(_.name)).getOrElse(s"Room $roomId")
        state.joinedRoom(roomId, roomName) -> logDebug(s"Joined room $roomId as $userId")

      case Some(ServerMessage.ReadySync(players)) =>
        state.withReadyState(players) -> logDebug(s"Ready sync: ${players.map(p => s"${p.userId}=${p.isReady}").mkString(", ")}")

      case Some(ServerMessage.BoardSync(currentTurn, board)) =>
        state.withBoard(currentTurn, board) -> logDebug(s"Board sync, turn: $currentTurn")

      case Some(ServerMessage.GameResult(winner)) =>
        state.withGameResult(winner) -> logDebug(s"Game result: ${winner.getOrElse("draw")}")

      case _ =>
        state -> logDebug(s"Unknown message: $value")

  private def logDebug(message: String): Cmd[IO, Msg] =
    Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Debug, message)))

  private def sendData(ws: WebSocket[IO], data: String): Cmd[IO, Nothing] =
    ws.publish(data)

  private def updateConnectionStatus(updatedStatus: ConnectionStatus)(using currentState: State): State =
    currentState.copy(connection = updatedStatus)

  private def connect(url: String): Cmd[IO, Msg] =
    WebSocket.connect[IO, Msg](address = url, keepAliveSettings = KeepAliveSettings.default) {
      case WebSocketConnect.Error(err) =>
        Msg.Connection(WebSocketMsg.Error(err))
      case WebSocketConnect.Socket(ws) =>
        Msg.Connection(WebSocketMsg.Connecting(ws))
    }
