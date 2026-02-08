package io.scrabit.example.tictactoe.msg.debug

import io.scrabit.example.tictactoe.msg.debug.DebugMsg.Level

case class DebugMsg(val level: Level, message: String)
object DebugMsg {
  enum Level:
    case Debug, Info, Warn, Error
}
