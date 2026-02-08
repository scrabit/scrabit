package io.scrabit.example.tictactoe

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import tyrian.websocket.*
import scala.scalajs.js.annotation.*
import scala.concurrent.duration.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.ui.UiMsg
import io.scrabit.example.tictactoe.handler.connection.ConnectionHandler
import io.scrabit.example.tictactoe.handler.debug.DebugHandler
import io.scrabit.example.tictactoe.handler.login.LoginHandler
import io.scrabit.example.tictactoe.handler.lobby.LobbyHandler
import io.scrabit.example.tictactoe.handler.game.GameHandler
import io.scrabit.example.tictactoe.handler.ui.UiHandler
import io.scrabit.example.tictactoe.view.login.LoginFormView
import io.scrabit.example.tictactoe.view.lobby.LobbyView
import io.scrabit.example.tictactoe.view.game.GameView
import io.scrabit.example.tictactoe.view.components.NotificationBanner
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, State]:

  def main(args: Array[String]): Unit = launch("app")

  def router: Location => Msg = Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (State, Cmd[IO, Msg]) =
    (State.init, Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Warn, "Hello world. App is init"))))

  def update(state: State): Msg => (State, Cmd[IO, Msg]) = msg =>
    handle(msg)(using state)

  private def handle(msg: Msg)(using State): (State, Cmd[IO, Msg]) = msg match
    case Msg.NoOp          => unchanged
    case Msg.Debug(m)      => DebugHandler(m)
    case Msg.Connection(m) => ConnectionHandler(m)
    case Msg.Login(m)      => LoginHandler(m)
    case Msg.Lobby(m)      => LobbyHandler(m)
    case Msg.Game(m)       => GameHandler(m)
    case Msg.Ui(m)         => UiHandler(m)

  private def unchanged(using State): (State, Cmd[IO, Msg]) = (summon[State], Cmd.None)

  def view(state: State): Html[Msg] =
    div()(
      NotificationBanner(state),
      div(cls := "center")(
        state match
          case s if s.isInGame =>
            GameView(s, s.game.get)
          case s if s.isLoggedIn =>
            LobbyView(s, s.lobby.get)
          case s =>
            LoginFormView(s)
      )
    )

  def subscriptions(state: State): Sub[IO, Msg] =
    val wsSub = state.connection.subscribe
    val notifSub =
      if state.notification.isDefined then
        Sub.every[IO](1.second, "notif-tick").map(_ => Msg.Ui(UiMsg.TickNotification))
      else Sub.None
    Sub.Batch[IO, Msg](wsSub, notifSub)
