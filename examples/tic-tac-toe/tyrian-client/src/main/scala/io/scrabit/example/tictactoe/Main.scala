package io.scrabit.example.tictactoe

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.websocket.*
import scala.scalajs.js.annotation.*
import io.circe.Json
import io.circe.syntax.*
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.handler.connection.ConnectionHandler
import io.scrabit.example.tictactoe.handler.debug.DebugHandler
import io.scrabit.example.tictactoe.handler.login.LoginHandler
import io.scrabit.example.tictactoe.view.login.LoginFormView
import io.scrabit.example.tictactoe.state.connection.ConnectionStatus
import io.scrabit.example.tictactoe.state.connection.subscribe
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, State]:

  def main(args: Array[String]): Unit = launch("app")

  def router: Location => Msg = Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (State, Cmd[IO, Msg]) =
    (State.init, Cmd.Emit(Msg.Debug(DebugMsg(DebugMsg.Level.Warn, "Hello world. App is init"))))

  def update(state: State): Msg => (State, Cmd[IO, Msg]) = { case msg: Msg =>
    handle(msg)(using state)
  }

  private def handle(msg: Msg)(using State): (State, Cmd[IO, Msg]) = msg match {
    case Msg.NoOp =>
      unchanged
    case Msg.Debug(debugMsg) =>
      DebugHandler(debugMsg)
    case Msg.Connection(ws) =>
      ConnectionHandler(ws)
    case Msg.Login(m) =>
      LoginHandler(m)
  }

  private def unchanged(using State): (State, Cmd[IO, Msg]) = (summon[State], Cmd.None)

  def view(state: State): Html[Msg] =
    LoginFormView(state)

  def subscriptions(state: State): Sub[IO, Msg] =
    state.connection match {
      case connecting: ConnectionStatus.Connecting =>
        connecting.subscribe {
          case WebSocketEvent.Error(e) =>
            Msg.Connection(WebSocketMsg.Error(e))
          case WebSocketEvent.Receive(message) =>
            Msg.Connection(WebSocketMsg.ReceiveData(message))
          case WebSocketEvent.Open =>
            Msg.Connection(WebSocketMsg.Connected)
          case WebSocketEvent.Close(code, reason) =>
            val error = s"<socket closed> - code: $code, reason: $reason"
            Msg.Connection(WebSocketMsg.Error(error))
          case WebSocketEvent.Heartbeat =>
            Msg.NoOp
        }
      case ConnectionStatus.Connected(ws) =>
        ws.subscribe {
          case WebSocketEvent.Error(e) =>
            Msg.Connection(WebSocketMsg.Error(e))
          case WebSocketEvent.Receive(message) =>
            Msg.Connection(WebSocketMsg.ReceiveData(message))
          case WebSocketEvent.Open =>
            Msg.Connection(WebSocketMsg.Connected)
          case WebSocketEvent.Close(code, reason) =>
            val error = s"<socket closed> - code: $code, reason: $reason"
            Msg.Connection(WebSocketMsg.Error(error))
          case WebSocketEvent.Heartbeat =>
            Msg.NoOp
        }
      case _ => Sub.None
    }
