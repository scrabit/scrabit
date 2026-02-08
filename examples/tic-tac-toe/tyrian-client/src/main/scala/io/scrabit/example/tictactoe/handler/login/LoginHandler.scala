package io.scrabit.example.tictactoe.handler.login

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.login.LoginMsg
import cats.effect.IO
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.state.auth.LoginFormModel
import tyrian.Cmd
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg

object LoginHandler extends MessageHandler[IO, LoginMsg] {
  protected def handle(message: LoginMsg, currentState: State): Result = {
    given State = currentState
    message match
    case LoginMsg.UpdateForm(username, password) =>
      currentState.copy(loginForm = LoginFormModel(username, password )) -> Cmd.None
    case LoginMsg.Submit =>
      currentState -> login(currentState.loginForm)
  }

  private def login(model: LoginFormModel): Cmd[IO,  Msg] = {
    val data = s"LOGIN-${model.username}/${model.password}"
    Cmd.Emit(Msg.Connection(WebSocketMsg.SendData(data)))
  }

}
