package io.scrabit.example.tictactoe.indigo.model

import indigo.*
import indigo.scenes.*

enum Mark:
  case X, O

case class Cell(mark: Option[Mark])

case class Board(cells: Vector[Vector[Cell]]):
  def get(x: Int, y: Int): Option[Cell] =
    cells.lift(y).flatMap(_.lift(x))

  def size: Int = cells.size

object Board:
  val empty: Board = Board(Vector.fill(3, 3)(Cell(None)))

case class Player(userId: String, isReady: Boolean)

case class Room(id: Int, owner: String, name: String)

case class LoginModel(
    username: String,
    password: String,
    focusedField: Int,
    error: Option[String]
)

object LoginModel:
  val initial: LoginModel = LoginModel("", "", 0, None)

case class LobbyModel(
    rooms: List[Room],
    newRoomName: String,
    typing: Boolean
)

object LobbyModel:
  val initial: LobbyModel = LobbyModel(Nil, "", false)

case class GameModel(
    roomName: String,
    roomId: Int,
    players: List[Player],
    board: Board,
    isMyTurn: Boolean,
    myMark: Option[Mark],
    started: Boolean,
    winner: Option[Option[String]]
)

object GameModel:
  def joining(roomId: Int, roomName: String): GameModel =
    GameModel(roomName, roomId, Nil, Board.empty, false, None, false, None)

case class Model(
    connection: ConnectionState,
    login: LoginModel,
    lobby: LobbyModel,
    game: Option[GameModel]
):
  def withSession(key: String): Model =
    copy(connection = connection.copy(sessionKey = Some(key), connected = true, username = login.username))

  def withRooms(rooms: List[Room]): Model =
    copy(lobby = lobby.copy(rooms = rooms))

  def joinRoom(roomId: Int, roomName: String): Model =
    copy(game = Some(GameModel.joining(roomId, roomName)))

  def withPlayers(players: List[Player]): Model =
    game match
      case None    => this
      case Some(g) => copy(game = Some(g.copy(players = players)))

  def withBoard(currentTurn: String, board: Board): Model =
    game match
      case None => this
      case Some(g) =>
        val myTurn = currentTurn == connection.username
        val mark = g.myMark.orElse {
          if currentTurn == connection.username then Some(Mark.X) else Some(Mark.O)
        }
        copy(game = Some(g.copy(board = board, isMyTurn = myTurn, started = true, myMark = mark)))

  def withResult(winner: Option[String]): Model =
    game match
      case None    => this
      case Some(g) => copy(game = Some(g.copy(winner = Some(winner))))

  def leaveGame: Model =
    copy(game = None)

  def withError(err: String): Model =
    copy(login = login.copy(error = Some(err)))

  def disconnected: Model =
    copy(connection = connection.copy(connected = false, sessionKey = None))

object Model:
  val initial: Model = Model(
    connection = ConnectionState.initial,
    login = LoginModel.initial,
    lobby = LobbyModel.initial,
    game = None
  )

case class ViewModel(
    cursorVisible: Boolean,
    lastBlink: Double,
    hoverCell: Option[(Int, Int)]
)

object ViewModel:
  val initial: ViewModel = ViewModel(true, 0d, None)
