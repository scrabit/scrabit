package io.scrabit.example.tictactoe.msg.lobby

import io.scrabit.example.tictactoe.state.game.Room

enum LobbyMsg:
  case UpdateRooms(rooms: List[Room])
  case UpdateNewRoomName(name: String)
  case CreateRoom
  case JoinRoom(roomId: Int)
