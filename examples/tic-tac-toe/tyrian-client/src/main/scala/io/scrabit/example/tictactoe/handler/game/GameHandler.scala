package io.scrabit.example.tictactoe.handler.game

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.game.GameMsg
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.model.Request
import cats.effect.IO
import tyrian.Cmd

object GameHandler extends MessageHandler[IO, GameMsg]:

  protected def handle(message: GameMsg, currentState: State): Result =
    message match
      case GameMsg.JoinedRoom(roomId, roomName) =>
        currentState.joinedRoom(roomId, roomName) -> Cmd.None

      case GameMsg.ToggleReady =>
        currentState.session match
          case Some(s) =>
            val req = Request.Ready(s.sessionKey)
            currentState -> Cmd.Emit(Msg.Connection(WebSocketMsg.SendData(req.toJson)))
          case None =>
            currentState -> Cmd.None

      case GameMsg.MakeMove(x, y) =>
        currentState.session match
          case Some(s) =>
            val req = Request.Move(x, y, s.sessionKey)
            currentState -> Cmd.Emit(Msg.Connection(WebSocketMsg.SendData(req.toJson)))
          case None =>
            currentState -> Cmd.None

      case GameMsg.UpdateReadyState(players) =>
        currentState.withReadyState(players) -> Cmd.None

      case GameMsg.UpdateBoard(currentTurn, board) =>
        currentState.withBoard(currentTurn, board) -> Cmd.None

      case GameMsg.GameResult(winner) =>
        currentState.withGameResult(winner) -> Cmd.None

      case GameMsg.BackToLobby =>
        currentState.leaveGame -> Cmd.None
