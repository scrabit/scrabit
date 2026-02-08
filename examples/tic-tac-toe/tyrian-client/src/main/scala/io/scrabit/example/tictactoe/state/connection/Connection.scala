package io.scrabit.example.tictactoe.state.connection

import tyrian.websocket.WebSocket
import cats.effect.IO
import tyrian.Sub
import tyrian.websocket.WebSocketEvent
import io.scrabit.example.tictactoe.msg.Msg 

enum ConnectionStatus:
  def isConnected: Boolean = this match {
    case Connected(_) => true
    case _ => false
  }
  def toConnected: ConnectionStatus = this match {
    case Connecting(ws) => Connected(ws)
    case _ => this
  }

  case Connecting(ws: WebSocket[IO])
  case Connected(s: WebSocket[IO])
  case ConnectionError(msg: String)
  case Disconnecting
  case Disconnected

extension (status: ConnectionStatus.Connecting)
  def subscribe(toMessage: WebSocketEvent => Msg): Sub[IO, Msg] =
    status.ws.subscribe(toMessage)
