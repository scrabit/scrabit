package io.scrabit.example.tictactoe.handler.connection

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.state.State
import tyrian.Cmd
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.msg.Msg
import cats.effect.IO
import tyrian.websocket.KeepAliveSettings
import tyrian.websocket.WebSocket
import tyrian.websocket.WebSocketConnect
import cats.effect.Async
import io.scrabit.example.tictactoe.msg.debug.DebugMsg

object ConnectionHandler extends MessageHandler[IO, WebSocketMsg] {
  private val backendUrl = "ws://localhost:8080"

  override protected def handle(message: WebSocketMsg, currentState: State): Result = {
    given State = currentState
    message match {
      case WebSocketMsg.Connect =>
        currentState -> connect(backendUrl)
      case WebSocketMsg.Connecting(ws) =>
        updateConnectionStatus(ConnectionStatus.Connecting(ws)) -> logDebug(s"Connecting to $backendUrl")
      case WebSocketMsg.Error(e) =>
        updateConnectionStatus(ConnectionStatus.ConnectionError(e)) -> Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Error, e)))
      case WebSocketMsg.Connected =>
        updateConnectionStatus(currentState.connection.toConnected) -> logDebug("Connected")
      case WebSocketMsg.Disconnected =>
        updateConnectionStatus(ConnectionStatus.Disconnected) ->
          logDebug("Disconnected")
      case WebSocketMsg.ReceiveData(value) =>
        currentState -> logDebug(s"ReceiveData: $value")
      case WebSocketMsg.SendData(data) =>
        currentState.connection match {
          case ConnectionStatus.Connected(ws) =>
            currentState -> sendData(ws, data)
          case _ =>
            currentState -> logDebug("Can't send data, websocket disconnected")
        }
    }
  }

  private def logDebug(message: String): Cmd[IO, Msg] =
    Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Debug, message)))

  private def logWarn(message: String): Cmd[IO, Msg] =
    Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Warn, message)))

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
}
