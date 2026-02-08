package io.scrabit.example.tictactoe.state

import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.state.auth.Session
import io.scrabit.example.tictactoe.state.auth.LoginFormModel

case class State(
  connection: ConnectionStatus,
  session: Option[Session],
  loginForm: LoginFormModel
)

object State {
  val init: State = State(
    connection = ConnectionStatus.Disconnected,
    session = None,
    loginForm = LoginFormModel.empty
  )
}
