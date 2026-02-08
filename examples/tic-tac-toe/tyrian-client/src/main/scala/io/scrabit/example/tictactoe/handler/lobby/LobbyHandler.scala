package io.scrabit.example.tictactoe.handler.lobby

import io.scrabit.example.tictactoe.handler.MessageHandler
import io.scrabit.example.tictactoe.msg.lobby.LobbyMsg
import io.scrabit.example.tictactoe.msg.Msg
import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.state.State
import io.scrabit.example.tictactoe.model.Request
import cats.effect.IO
import tyrian.Cmd

object LobbyHandler extends MessageHandler[IO, LobbyMsg]:

  protected def handle(message: LobbyMsg, currentState: State): Result =
    message match
      case LobbyMsg.UpdateRooms(rooms) =>
        currentState.withRooms(rooms) -> Cmd.None

      case LobbyMsg.UpdateNewRoomName(name) =>
        val updated = currentState.lobby match
          case Some(lb) => currentState.copy(lobby = Some(lb.copy(newRoomName = name)))
          case None     => currentState
        updated -> Cmd.None

      case LobbyMsg.CreateRoom =>
        currentState.lobby match
          case Some(lb) if lb.newRoomName.trim.nonEmpty =>
            val req = Request.CreateRoom(lb.newRoomName.trim, lb.sessionKey)
            val cleared = currentState.copy(lobby = Some(lb.copy(newRoomName = "")))
            cleared -> Cmd.Emit(Msg.Connection(WebSocketMsg.SendData(req.toJson)))
          case _ =>
            currentState -> Cmd.None

      case LobbyMsg.JoinRoom(roomId) =>
        currentState.lobby match
          case Some(lb) =>
            val req = Request.JoinRoom(roomId, lb.sessionKey)
            currentState -> Cmd.Emit(Msg.Connection(WebSocketMsg.SendData(req.toJson)))
          case None =>
            currentState -> Cmd.None
