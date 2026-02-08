package io.scrabit.example.tictactoe.handler.ui

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.ui.UiMsg
import io.scrabit.example.tictactoe.state.State
import cats.effect.IO
import tyrian.Cmd

object UiHandler extends MessageHandler[IO, UiMsg]:

  protected def handle(message: UiMsg, currentState: State): Result =
    message match
      case UiMsg.DismissNotification =>
        currentState.clearNotification -> Cmd.None

      case UiMsg.TickNotification =>
        currentState.notification match
          case Some(n) if n.ttl <= 1 =>
            currentState.clearNotification -> Cmd.None
          case Some(n) =>
            currentState.copy(notification = Some(n.copy(ttl = n.ttl - 1))) -> Cmd.None
          case None =>
            currentState -> Cmd.None
