package io.scrabit.example.tictactoe.msg.connection

import tyrian.websocket.WebSocket
import cats.effect.IO

enum WebSocketMsg {
  case Connect
  case Error(error: String)
  case Disconnected(reason: String)
  case Connecting(ws: WebSocket[IO])
  case ReceiveData(value: String)
  case SendData(value: String)
  case Connected
}
