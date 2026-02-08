package io.scrabit.example.tictactoe.msg.game

import io.scrabit.example.tictactoe.state.game.*

enum GameMsg:
  case ToggleReady
  case MakeMove(x: Int, y: Int)
  case UpdateReadyState(players: List[Player])
  case UpdateBoard(currentTurn: String, board: Board)
  case GameResult(winner: Option[String])
  case JoinedRoom(roomId: Int, roomName: String)
  case BackToLobby
