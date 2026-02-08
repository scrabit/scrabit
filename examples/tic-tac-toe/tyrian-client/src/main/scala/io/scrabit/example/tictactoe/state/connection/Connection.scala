package io.scrabit.example.tictactoe.state.connection

import tyrian.websocket.WebSocket
import cats.effect.IO
import tyrian.Sub
import tyrian.websocket.WebSocketEvent
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg

enum ConnectionStatus:
  def isConnected: Boolean = this match {
    case Connected(_) => true
    case _            => false
  }

  def toConnected: ConnectionStatus = this match {
    case Connecting(ws) => Connected(ws)
    case _              => this
  }

  def subscribe: Sub[IO, Msg] = this match {
    case Connecting(ws) => ws.subscribe(eventToMsg)
    case Connected(ws)  => ws.subscribe(eventToMsg)
    case _              => Sub.None
  }

  private def eventToMsg(event: WebSocketEvent): Msg = event match {
    case WebSocketEvent.Error(e) =>
      Msg.Connection(WebSocketMsg.Error(e))
    case WebSocketEvent.Receive(message) =>
      Msg.Connection(WebSocketMsg.ReceiveData(message))
    case WebSocketEvent.Open =>
      Msg.Connection(WebSocketMsg.Connected)
    case WebSocketEvent.Close(code, reason) =>
      if code == 1000 then
        Msg.Connection(WebSocketMsg.Disconnected(reason))
      else
        Msg.Connection(WebSocketMsg.Error(s"<socket closed> - code: $code, reason: $reason"))
    case WebSocketEvent.Heartbeat =>
      Msg.NoOp
  }

  case Connecting(ws: WebSocket[IO])
  case Connected(ws: WebSocket[IO])
  case ConnectionError(msg: String)
  case Disconnecting
  case Disconnected
