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

package object model {

  case class State(loginState: LoginState, echoSocket: Socket, log: List[String], lobby: Option[Lobby], game: Option[Game]) {
    def loggedIn: Boolean = lobby.nonEmpty
    def updateRoomList(rooms: List[Room]): State =
      lobby match
        case None =>
          copy(lobby = Some(Lobby(rooms, "")))
        case Some(lb) =>
          copy(lobby = Some(lb.copy(rooms = rooms)))
    def updateSessionKey(key: String): State =
      lobby match
        case None =>
          copy(lobby = Some(Lobby(Nil, key)))
        case Some(lb) =>
          copy(lobby = Some(lb.copy(sessionKey = key)))

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
      // Could reset to lobby or show game over screen
      this
  }

  object State {

    case class LoginState(username: String, password: String, sessionKey: Option[String]) {
      def isLoggedIn: Boolean = sessionKey.nonEmpty
    }

    case class Room(id: Int, owner: String, name: String)
    case class Lobby(rooms: List[Room], sessionKey: String)
    case class Game(roomName: String, roomId: Int, players: List[Player], isMyTurn: Boolean, board: Board)
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
      val emptyLoginState = LoginState("", "", None)
      State(emptyLoginState, Socket.init, Nil, None, None)
  }

  case class Socket(socketUrl: String, socket: Option[WebSocket[IO]]):
    def connectDisconnectButton =
      if socket.isDefined then button(onClick(Socket.Status.Disconnecting.asMsg))("Disconnect")
      else button(onClick(Socket.Status.Connecting.asMsg))("Connect")

    def update(status: Socket.Status): (Socket, Cmd[IO, Msg]) =
      status match
        case Socket.Status.ConnectionError(err) =>
          (this, Logger.error(s"Failed to open WebSocket connection: $err"))

        case Socket.Status.Connected(ws) =>
          (this.copy(socket = Some(ws)), Cmd.None)

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

          (this, connect)

        case Socket.Status.Disconnecting =>
          val log = Logger.info[IO]("Graceful shutdown of EchoSocket connection")
          val cmds =
            socket.map(ws => Cmd.Batch(log, ws.disconnect)).getOrElse(log)

          (this.copy(socket = None), cmds)

        case Socket.Status.Disconnected =>
          (this, Logger.info("WebSocket not connected yet"))

    def publish(message: String): Cmd[IO, Msg] =
      socket.map(_.publish(message)).getOrElse(Cmd.None)

    def subscribe(toMessage: WebSocketEvent => Msg): Sub[IO, Msg] =
      socket.fold(Sub.emit[IO, Msg](Socket.Status.Disconnected.asMsg)) {
        _.subscribe(toMessage)
      }

  object Socket:
    val init: Socket =
      Socket("ws://localhost:8080/", None)

    enum Status:
      case Connecting
      case Connected(ws: WebSocket[IO])
      case ConnectionError(msg: String)
      case Disconnecting
      case Disconnected

      def asMsg: Msg = Msg.WebSocketStatus(this)

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
    case NoOp
}
