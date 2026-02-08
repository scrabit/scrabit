package io.scrabit.example.tictactoe.msg

import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg
import io.scrabit.example.tictactoe.msg.login.LoginMsg
import io.scrabit.example.tictactoe.msg.lobby.LobbyMsg
import io.scrabit.example.tictactoe.msg.game.GameMsg

enum Msg:
  case NoOp
  case Debug(msg: DebugMsg)
  case Connection(ws: WebSocketMsg)
  case Login(msg: LoginMsg)
  case Lobby(msg: LobbyMsg)
  case Game(msg: GameMsg)
