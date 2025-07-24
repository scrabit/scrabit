package io.scrabit.example.tictactoe.view

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.model.State.*
import io.scrabit.example.tictactoe.model.Msg
import io.scrabit.example.tictactoe.model.State
import io.scrabit.example.tictactoe.model.Socket

object Menu {

  def loginForm(state: LoginState): Html[Msg] =
    div(cls := "login-form nes-container with-title")(
      h3(cls := "title")("Login to Tic-Tac-Toe"),
      div(cls := "nes-field")(
        label(htmlFor := "username")("Username:"),
        input(
          id          := "username",
          cls         := "nes-input",
          placeholder := "Enter username...",
          value       := state.username,
          onInput(username => Msg.UpdateLoginState(state.copy(username = username)))
        )
      ),
      div(cls := "nes-field")(
        label(htmlFor := "password")("Password:"),
        input(
          id          := "password",
          cls         := "nes-input",
          `type`      := "password",
          placeholder := "Enter password...",
          value       := state.password,
          onInput(password => Msg.UpdateLoginState(state.copy(password = password)))
        )
      ),
      div(cls := "login-buttons")(
        button(
          cls := "nes-btn is-primary",
          onClick(Msg.Login),
          disabled(state.username.trim.isEmpty || state.password.trim.isEmpty)
        )("Login")
      )
    )

  def apply(state: State): Html[Msg] =
    val websocketButton = if state.echoSocket.isConnected then div() else button(onClick(Socket.Status.Connecting.asMsg))("Connect")

    div(cls := "login-screen")(
      div(cls := "connection-section")(
        h2("Tic-Tac-Toe Game"),
        websocketButton,
        if (state.echoSocket.isConnected) {
          loginForm(state.loginState)
        } else {
          div(cls := "nes-container")(
            p("Connect to the server to start playing!")
          )
        }
      ),
      if (state.log.nonEmpty) {
        div(cls := "debug-log nes-container")(
          h4("Connection Log:"),
          div(cls := "log-messages")(
            state.log.take(5).map(msg => p(msg))*
          )
        )
      } else {
        div()
      }
    )
}
