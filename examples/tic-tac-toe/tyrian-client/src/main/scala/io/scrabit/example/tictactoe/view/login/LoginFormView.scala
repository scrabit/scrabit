package io.scrabit.example.tictactoe.view.login

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.login.LoginMsg
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.state.auth.LoginFormModel
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg

object LoginFormView {

  def apply(state: State): Html[Msg] =
    val websocketButton = if (state.connection.isConnected) then div() else button(onClick(Msg.Connection(WebSocketMsg.Connect)))("Connect")

    div(cls := "login-screen")(
      div(cls := "connection-section")(
        h2("Tic-Tac-Toe Game"),
        websocketButton,
        if (state.connection.isConnected) {
          loginForm(state.loginForm)
        } else {
          div(cls := "nes-container")(
            p("Connect to the server to start playing!")
          )
        }
      )
    )

  def loginForm(model: LoginFormModel): Html[Msg] =
    div(cls := "login-form nes-container with-title")(
      h3(cls := "title")("Login to Tic-Tac-Toe"),
      div(cls := "nes-field")(
        label(htmlFor := "username")("Username:"),
        input(
          id          := "username",
          cls         := "nes-input",
          placeholder := "Enter username...",
          value       := model.username,
          onInput(username => Msg.Login(LoginMsg.UpdateForm(username, model.password)))
        )
      ),
      div(cls := "nes-field")(
        label(htmlFor := "password")("Password:"),
        input(
          id          := "password",
          cls         := "nes-input",
          `type`      := "password",
          placeholder := "Enter password...",
          value       := model.password,
          onInput(password => Msg.Login(LoginMsg.UpdateForm(model.username, password)))
        )
      ),
      div(cls := "login-buttons")(
        button(
          cls := "nes-btn is-primary",
          onClick(Msg.Login(LoginMsg.Submit)),
          disabled(model.username.trim.isEmpty || model.password.trim.isEmpty)
        )("Login")
      )
    )
}
