package io.scrabit.example.tictactoe.view

import tyrian.Html
import tyrian.Html.*
import io.scrabit.example.tictactoe.model.State.*
import io.scrabit.example.tictactoe.model.Msg
import io.scrabit.example.tictactoe.model.State

object Menu {

  def loginForm(state: LoginState): Html[Msg] = 
    div(
      h1(s"Username: ${state.username}"),
      h1(s"Password: ${state.password.map(_ => '*')}"),
      div(input(onChange(v => Msg.UpdateLoginState(state.copy(username = v))))),
      p(button(cls := "center", onClick(Msg.Login))("Login"))
    )

  def apply(state: State): Html[Msg] = 
    if (state.loginState.isLoggedIn){
        div(
          h1(s"Login as ${state.loginState.username}")
          )
    } else {
      div(
        div(
          state.echoSocket.connectDisconnectButton,
          p("Log:"),
          p(button(cls := "button text-green-800")("Create room")),
          p(state.log.flatMap(msg => List(text(msg), br)))
        )
      )

    }

}
