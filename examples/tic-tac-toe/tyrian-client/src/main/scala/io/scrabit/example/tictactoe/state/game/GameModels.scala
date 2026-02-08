package io.scrabit.example.tictactoe.state.game

enum Mark:
  case X, O

case class Cell(mark: Option[Mark])

case class Board(cells: Vector[Vector[Cell]]):
  def get(x: Int, y: Int): Option[Cell] =
    cells.lift(y).flatMap(_.lift(x))

object Board:
  val empty: Board = Board(Vector.fill(3, 3)(Cell(None)))

case class Player(userId: String, isReady: Boolean)

case class Room(id: Int, owner: String, name: String)

case class Lobby(rooms: List[Room], sessionKey: String, newRoomName: String)

case class Game(
    roomName: String,
    roomId: Int,
    players: List[Player],
    isMyTurn: Boolean,
    board: Board,
    started: Boolean = false,
    winner: Option[String] = None,
    myMark: Option[Mark] = None
):
  def isGameOver: Boolean = winner.isDefined || isBoardFull
  def isBoardFull: Boolean = board.cells.forall(row => row.forall(cell => cell.mark.isDefined))
  def allReady: Boolean = players.nonEmpty && players.forall(_.isReady)
