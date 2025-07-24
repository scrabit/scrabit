package io.scrabit.example.tictactoe

import tyrian.websocket.WebSocket
import cats.effect.IO
import tyrian.Html.*
import tyrian.Cmd
import tyrian.cmds.Logger
import io.circe.Decoder
import io.circe.*
import io.circe.parser.*
import io.circe.generic.semiauto.*
import tyrian.websocket.KeepAliveSettings
import tyrian.websocket.WebSocketConnect
import tyrian.websocket.WebSocketEvent
import tyrian.Sub
import io.scrabit.example.tictactoe.model.*
import io.scrabit.example.tictactoe.model.State.*
import cats.effect.std.Dispatcher
import java.awt.print.Book

package object model {

  case class State(loginState: LoginState, echoSocket: Socket, log: List[String], lobby: Option[Lobby], game: Option[Game]) {
    def loggedIn: Boolean = lobby.nonEmpty
    def updateRoomList(rooms: List[Room]): State =
      lobby match
        case None =>
          copy(lobby = Some(Lobby(rooms, "", "")))
        case Some(lb) =>
          copy(lobby = Some(lb.copy(rooms = rooms)))
    def updateSessionKey(key: String): State =
      val updatedLoginState = loginState.copy(sessionKey = Some(key))
      val updatedLobby = lobby match
        case None =>
          Some(Lobby(Nil, key, ""))
        case Some(lb) =>
          Some(lb.copy(sessionKey = key))
      copy(loginState = updatedLoginState, lobby = updatedLobby)

    def joinedRoom(roomId: Int, roomName: String): State =
      copy(game = Some(Game(roomName = roomName, roomId = roomId, players = Nil, isMyTurn = false, Board.empty)))

    def updateReadyState(players: List[Player]): State =
      game match
        case None => this
        case Some(g) => copy(game = Some(g.copy(players = players)))

    def updateBoard(currentTurn: String, board: Board): State =
      game match
        case None => this
        case Some(g) => 
          val isMyTurn = currentTurn == loginState.username
          copy(game = Some(g.copy(board = board, isMyTurn = isMyTurn)))

    def gameEnded(winner: Option[String]): State =
      game match
        case None => this
        case Some(g) => copy(game = Some(g.copy(winner = winner)))
  }

  object State {

    case class LoginState(username: String, password: String, sessionKey: Option[String]) {
      def isLoggedIn: Boolean = sessionKey.nonEmpty
    }

    case class Room(id: Int, owner: String, name: String)
    case class Lobby(rooms: List[Room], sessionKey: String, newRoomName: String)
    case class Game(roomName: String, roomId: Int, players: List[Player], isMyTurn: Boolean, board: Board, winner: Option[String] = None) {
      def isGameOver: Boolean = winner.isDefined
    }
    case class Player(userId: String, isReady: Boolean)

    enum Mark:
      case X, O

    case class Cell(mark: Option[Mark])

    case class Board(cells: Vector[Vector[Cell]])

    object Board {
      val empty: Board            = Board(13)
      def apply(size: Int): Board = Board(Vector.fill(size, size)(Cell(None)))
    }

    val init: State =
      val emptyLoginState = LoginState("rabbit1", "sc@la", None)
      State(emptyLoginState, Socket.init, Nil, None, None)
  }

  case class Socket(socketUrl: String, status: Socket.Status):
    def isConnected: Boolean = status match
       case Socket.Status.Connected(_) => 
         true
       case _ =>
         false
    

    def update(status: Socket.Status): (Socket, Cmd[IO, Msg]) =
      status match
        case Socket.Status.ConnectionError(err) =>
          (this, Logger.error(s"Failed to open WebSocket connection: $err"))

        case connected @ Socket.Status.Connected(_) =>
          (this.copy(status = connected), Logger.error("wrong..."))

        case Socket.Status.Connecting =>
          val connect =
            WebSocket.connect[IO, Msg](
              address = socketUrl,
              keepAliveSettings = KeepAliveSettings.default
            ) {
              case WebSocketConnect.Error(err) =>
                Socket.Status.ConnectionError(err).asMsg

              case WebSocketConnect.Socket(ws) =>
                Socket.Status.Connected(ws).asMsg
            }

          (this.copy(status = Socket.Status.Connecting), connect)

        case Socket.Status.Disconnecting =>
          val cmd = this.status match
              case Socket.Status.Connected(ws) =>
                  ws.disconnect
              case _ => Cmd.None
          (this.copy(status = Socket.Status.Disconnecting), cmd)

        case Socket.Status.Disconnected =>
          (this, Logger.info("WebSocket not connected yet"))

    def publish(message: String): Cmd[IO, Msg] =
      status.ws.map(_.publish(message)).getOrElse(Cmd.None)

    def subscribe(toMessage: WebSocketEvent => Msg): Sub[IO, Msg] =
      status.ws.fold(Sub.emit[IO, Msg](Socket.Status.Disconnected.asMsg)) {
        _.subscribe(toMessage)
      }

  object Socket:
    val init: Socket =
      Socket("ws://localhost:8080/", Status.Disconnected)

    enum Status:
      case Connecting
      case Connected(connection: WebSocket[IO])
      case ConnectionError(msg: String)
      case Disconnecting
      case Disconnected

      def asMsg: Msg = Msg.WebSocketStatus(this)

      def ws: Option[WebSocket[IO]] = this match
         case Connected(ws) =>
           Some(ws)
         case _ => 
           None

      

  enum Msg:
    case WebSocketStatus(status: Socket.Status)
    case FromSocket(message: String)
    case Login
    case CreateRoom(name: String)
    case UpdateLoginState(state: LoginState)
    case UpdateLobby(rooms: List[Room])
    case JoinRoom(id: Int, name: String)
    case SendRequest(request: String)
    case UpdateSessionKey(key: String)
    case UpdateReadyState(players: List[Player])
    case UpdateBoard(currentTurn: String, board: Board)
    case GameResult(winner: Option[String])
    case ToggleReady           // NEW: ready/unready for game
    case MakeMove(x: Int, y: Int) // NEW: place X/O on board
    case BackToLobby           // NEW: leave game room
    case JoinSpecificRoom(roomId: Int) // NEW: join a specific room
    case UpdateNewRoomName(name: String) // NEW: update new room name input
    case UserJoinedRoom(roomId: Int, userId: String) // NEW: track when other users join rooms
    case RestartGame // NEW: restart the current game
    case NoOp
}
