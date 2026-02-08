package io.scrabit.example.tictactoe.handler

import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.msg.Msg

import tyrian.Cmd
import cats.effect.IO

trait MessageHandler[F[_], M] {
  type Result = (State, Cmd[F, Msg])
  protected def handle(message: M, currentState: State): Result

  def apply(message: M)(using currentState: State): Result = {
    handle(message, currentState)
  }
}
