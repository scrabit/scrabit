package io.scrabit.example.tictactoe.handler.debug

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.debug.DebugMsg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg.Level
import io.scrabit.example.tictactoe.state.State
import tyrian.Cmd
import cats.effect.IO

import scala.scalajs.js

object DebugHandler extends MessageHandler[IO, DebugMsg] {

  override protected def handle(message: DebugMsg, currentState: State): Result = {
    (currentState, Cmd.SideEffect(logToConsole(message)))
  }

  private def logToConsole(msg: DebugMsg): IO[Unit] = IO {
    val (color, prefix) = msg.level match {
      case Level.Debug => ("#6B7280", "🔍 DEBUG")  // Gray
      case Level.Info  => ("#3B82F6", "ℹ️ INFO")   // Blue
      case Level.Warn  => ("#F59E0B", "⚠️ WARN")   // Orange
      case Level.Error => ("#EF4444", "❌ ERROR")  // Red
    }

    val style = s"color: $color; font-weight: bold;"
    js.Dynamic.global.console.log(s"%c$prefix%c ${msg.message}", style, "")
  }
}
