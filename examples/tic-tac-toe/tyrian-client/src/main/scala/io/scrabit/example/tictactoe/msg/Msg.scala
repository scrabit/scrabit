package io.scrabit.example.tictactoe.msg

import io.scrabit.example.tictactoe.msg.connection.WebSocketMsg
import io.scrabit.example.tictactoe.msg.debug.DebugMsg
import io.scrabit.example.tictactoe.msg.login.LoginMsg

enum Msg:
    case NoOp
    case Debug(msg: DebugMsg)
    case Connection(ws: WebSocketMsg)
    // case WebSocketStatus(status: Socket.Status)
    // case FromSocket(message: String)
    case Login(msg: LoginMsg)
    // case CreateRoom(name: String)
    // case UpdateLoginState(state: LoginState)
    // case UpdateLobby(rooms: List[Room])
    // case JoinRoom(id: Int, name: String)
    // case SendRequest(request: String)
    // case UpdateSessionKey(key: String)
    // case UpdateReadyState(players: List[Player])
    // case UpdateBoard(currentTurn: String, board: Board)
    // case GameResult(winner: Option[String])
    // case ToggleReady           // NEW: ready/unready for game
    // case MakeMove(x: Int, y: Int) // NEW: place X/O on board
    // case BackToLobby           // NEW: leave game room
    // case JoinSpecificRoom(roomId: Int) // NEW: join a specific room
    // case UpdateNewRoomName(name: String) // NEW: update new room name input
    // case UserJoinedRoom(roomId: Int, userId: String) // NEW: track when other users join rooms
    // case RestartGame // NEW: restart the current game
