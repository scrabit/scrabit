package tictactoe.logic

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.scrabit.actor.message.OutgoingMessage
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.message.RoomMessage
import io.scrabit.actor.message.IncomingMessage.Request
import io.scrabit.actor.message.RoomMessage.Action
import GameLogic.Msg.*
import io.scrabit.actor.CommunicationHub.CommunicationHubServiceKey
import io.scrabit.actor.CommunicationHub
import io.circe.Json
import io.circe.syntax.*
import tictactoe.logic.GameLogic.*

final private class GameLogic(hub: ActorRef[OutgoingMessage]) {

  def waiting(state: Waiting): Behavior[Msg] = Behaviors.receiveMessagePartial {
    case Ready(username) =>
      val updatedState = state.readyToggle(username)
      if updatedState.canStart then {
        val firstTurn    = updatedState.player1
        val playingState = Playing(updatedState.player1, updatedState.player2.get, Board.empty, firstTurn)
        hub ! BoardSync(playingState.player1.userId, firstTurn, playingState.board)
        hub ! BoardSync(playingState.player2.userId, firstTurn, playingState.board)
        playing(playingState)
      } else waiting(updatedState)

    case Join(player) =>
      waiting(state.copy(player2 = Some(player)))
  }

  private def playing(state: Playing): Behavior[Msg] = Behaviors.receiveMessage { case Move(player, x, y) =>
    val isCorrectTurn = player == state.currentTurn
    if isCorrectTurn then {
      state.board.move(state.turnMark, x, y) match
        case None =>
          Behaviors.same
        case Some(updatedBoard) =>
          val updatedState = state.copy(board = updatedBoard).nextTurn

          hub ! BoardSync(state.player1.userId, updatedState.currentTurn, updatedBoard)
          hub ! BoardSync(state.player2.userId, updatedState.currentTurn, updatedBoard)

          if updatedBoard.currentTurnWon then {
            hub ! GameResult(state.player1.userId, Some(state.currentTurn))
            hub ! GameResult(state.player2.userId, Some(state.currentTurn))
            waiting(state.toWaiting)
          } else if (updatedBoard.isFull) {
            hub ! GameResult(state.player1.userId, None)
            hub ! GameResult(state.player2.userId, None)
            waiting(state.toWaiting)
          } else {
            playing(updatedState)
          }

    } else Behaviors.same

  }

}

object GameLogic:

  sealed trait GameState

  case class Waiting(player1: Player, player2: Option[Player] = None) extends GameState {
    def canStart: Boolean             = player1.isReady && player2.exists(_.isReady)
    def join(player: Player): Waiting = this.copy(player2 = Some(player))
    def readyToggle(username: String): Waiting =
      player2 match
        case Some(p2) if p2.userId == username => copy(player2 = player2.map(_.readyToggle))
        case _ if player1.userId == username   => copy(player1 = player1.readyToggle)
        case _                                 => this
  }

  enum Mark:
    case X
    case O

  case class Cell(mark: Option[Mark])

  case class Board(cells: Vector[Vector[Cell]]) {

    def currentTurnWon: Boolean = {
      def checkLine(line: Vector[Cell]): Boolean =
        line.forall(_.mark == line.head.mark) && line.head.mark.nonEmpty

      // Check rows
      val rowsWon = cells.exists(checkLine)

      // Check columns
      val colsWon = cells.indices.exists { colIndex =>
        checkLine(cells.map(row => row(colIndex)))
      }

      // Check diagonals
      val mainDiagonalWon = checkLine(cells.indices.map(i => cells(i)(i)).toVector)
      val antiDiagonalWon = checkLine(cells.indices.map(i => cells(i)(cells.size - 1 - i)).toVector)

      rowsWon || colsWon || mainDiagonalWon || antiDiagonalWon
    }

    def isFull: Boolean =
      cells.forall(_.forall(_.mark.nonEmpty))

    def move(mark: Mark, x: Int, y: Int): Option[Board] =
      // return None if cell is already occupied
      cells.lift(y).flatMap(row => row.lift(x)) match
        case None =>
          None
        case Some(cell) =>
          if cell.mark.isEmpty
          then Some(copy(cells = cells.updated(y, cells(y).updated(x, Cell(Some(mark))))))
          else None

  }

  object Board {
    val empty: Board            = Board(13)
    def apply(size: Int): Board = Board(Vector.fill(size, size)(Cell(None)))
  }

  case class Playing(player1: Player, player2: Player, board: Board, currentTurn: Player) extends GameState {
    def toWaiting: Waiting = Waiting(player1.readyToggle, Some(player2.readyToggle))
    def turnMark: Mark =
      if currentTurn == player1 then Mark.X else Mark.O

    def nextTurn: Playing =
      val turn = if currentTurn == player1 then player2 else player1
      copy(currentTurn = turn)
  }

  case object Finished extends GameState

  case class Player(userId: String, isReady: Boolean = false) {
    def readyToggle: Player = copy(isReady = !isReady)

    override def equals(obj: Any): Boolean = obj match {
      case Player(username, _) => this.userId == username
      case _                   => false
    }
  }

  enum Msg:
    case Join(player: Player)
    case Ready(username: String)
    case Move(player: Player, x: Int, y: Int)

  case class BoardSync(userId: String, currentTurn: Player, board: Board) extends OutgoingMessage {
    override val tpe: Int = 13

    override def data: Json =
      val rows: Vector[Json] = board.cells.map { row =>
        Json.arr(row.map {
          case Cell(Some(Mark.X)) => "X".asJson
          case Cell(Some(Mark.O)) => "O".asJson
          case Cell(None)         => Json.Null
        }*)
      }

      Json.obj(
        "currentTurn" -> currentTurn.userId.asJson,
        "cells"       -> Json.arr(rows*)
      )
  }

  case class GameResult(userId: String, winner: Option[Player]) extends OutgoingMessage {

    override def tpe: Int = 14

    override def data: Json = Json.obj("winner" -> winner.map(_.userId).asJson)

  }

  private def apply(hub: ActorRef[OutgoingMessage], username: String): Behavior[Msg] = Behaviors.setup { context =>
    val player1     = Player(username)
    val intialState = Waiting(player1, None)
    new GameLogic(hub).waiting(intialState)
  }

  def adapter(hub: ActorRef[OutgoingMessage], username: String): Behavior[RoomMessage] = Behaviors.setup(context =>
    val gameLogic = context.spawnAnonymous(GameLogic(hub, username))
    val convert: RoomMessage => Msg = {
      case Action.JoinRoom(userId) =>
        Msg.Join(Player(userId))
      case Action.Ready(userId) =>
        Msg.Ready(userId)
    }
    Behaviors.receiveMessage[RoomMessage] { msg =>
      gameLogic ! convert(msg)
      Behaviors.same
    }
  )
