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
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import tictactoe.message.out.BoardSync
import tictactoe.message.out.GameResult
import tictactoe.message.out.ReadySync

final private class GameLogic(hub: ActorRef[OutgoingMessage], context: ActorContext[Msg]) {

  private val logger = org.slf4j.LoggerFactory.getLogger("Game")

  def waiting(state: Waiting): Behavior[Msg] = Behaviors.receiveMessagePartial {
    case ReadyToggle(userId) =>
      logger.info(s"user $userId readyToggle")

      val updatedState = state.readyToggle(userId)

      updatedState.players foreach { player =>
        hub ! ReadySync(player.userId, updatedState.players)
      }

      if updatedState.canStart then {
        val firstTurn    = updatedState.player1
        val playingState = Playing(updatedState.player1, updatedState.player2.get, Board.empty, firstTurn)
        hub ! BoardSync(playingState.player1.userId, firstTurn, playingState.board)
        hub ! BoardSync(playingState.player2.userId, firstTurn, playingState.board)
        playing(playingState)
      } else waiting(updatedState)

    case Join(player) =>
      logger.info(s"🎮 Player ${player.userId} joined the room")
      val updatedState = state.copy(player2 = Some(player))
      
      // Send ReadySync to all players so everyone knows about the current player list
      logger.info(s"📤 Broadcasting player list update to ${updatedState.players.size} players: ${updatedState.players.map(_.userId).mkString(", ")}")
      updatedState.players foreach { p =>
        hub ! ReadySync(p.userId, updatedState.players)
      }
      
      waiting(updatedState)
  }

  private def playing(state: Playing): Behavior[Msg] = Behaviors.receiveMessage { case Msg.Move(player, x, y) =>
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
    def players: List[Player]         = List(player1) ++ player2.toList
    def join(player: Player): Waiting = this.copy(player2 = Some(player))
    def readyToggle(userId: String): Waiting =
      player2 match
        case Some(p2) if p2.userId == userId => copy(player2 = player2.map(_.readyToggle))
        case _ if player1.userId == userId   => copy(player1 = player1.readyToggle)
        case _                               => this
  }

  enum Mark:
    case X
    case O

  case class Cell(mark: Option[Mark])

  case class Board(cells: Vector[Vector[Cell]]) {

    def currentTurnWon: Boolean = {
      val size = cells.size
      val winLength = 5

      def isValidPos(x: Int, y: Int): Boolean = x >= 0 && x < size && y >= 0 && y < size

      def getCell(x: Int, y: Int): Option[Mark] = 
        if (isValidPos(x, y)) cells(y)(x).mark else None

      def findLongestSequence(startX: Int, startY: Int, dx: Int, dy: Int): (Int, Int, Int) = {
        // Find the start of the entire sequence (go backwards first)
        val mark = getCell(startX, startY)
        if (mark.isEmpty) return (0, 0, 0)

        // Find the actual start of the sequence by going backwards
        var seqStartX = startX
        var seqStartY = startY
        while (isValidPos(seqStartX - dx, seqStartY - dy) && getCell(seqStartX - dx, seqStartY - dy) == mark) {
          seqStartX -= dx
          seqStartY -= dy
        }

        // Count the full sequence length from the actual start
        var count = 0
        var x = seqStartX
        var y = seqStartY
        while (isValidPos(x, y) && getCell(x, y) == mark) {
          count += 1
          x += dx
          y += dy
        }

        (count, seqStartX, seqStartY)
      }

      def checkDirection(startX: Int, startY: Int, dx: Int, dy: Int): Boolean = {
        val mark = getCell(startX, startY)
        if (mark.isEmpty) return false

        val (sequenceLength, seqStartX, seqStartY) = findLongestSequence(startX, startY, dx, dy)
        
        if (sequenceLength >= winLength) {
          // Check if both ends of the ENTIRE sequence are blocked by opponent pieces
          val beforeX = seqStartX - dx
          val beforeY = seqStartY - dy
          val afterX = seqStartX + (sequenceLength * dx)
          val afterY = seqStartY + (sequenceLength * dy)
          
          val beforeBlocked = getCell(beforeX, beforeY).exists(m => m != mark.get)
          val afterBlocked = getCell(afterX, afterY).exists(m => m != mark.get)
          
          // Win if not blocked on both ends
          !(beforeBlocked && afterBlocked)
        } else {
          false
        }
      }

      // Check all positions and directions
      val directions = Vector((1, 0), (0, 1), (1, 1), (1, -1)) // horizontal, vertical, diagonal, anti-diagonal

      // Track checked sequences to avoid duplicates
      val checkedSequences = scala.collection.mutable.Set[(Int, Int, Int, Int)]()

      // Check if any position has a winning sequence
      (for {
        y <- 0 until size
        x <- 0 until size
        (dx, dy) <- directions
        if !checkedSequences.contains((x, y, dx, dy))
      } yield {
        val result = checkDirection(x, y, dx, dy)
        if (getCell(x, y).nonEmpty) {
          // Mark this entire sequence as checked by finding its start and marking all positions
          val (_, seqStartX, seqStartY) = findLongestSequence(x, y, dx, dy)
          var cx = seqStartX
          var cy = seqStartY
          while (isValidPos(cx, cy) && getCell(cx, cy) == getCell(x, y)) {
            checkedSequences.add((cx, cy, dx, dy))
            cx += dx
            cy += dy
          }
        }
        result
      }).exists(identity)
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

  case class Player(userId: String, isReady: Boolean = false) {
    def readyToggle: Player = copy(isReady = !isReady)

    override def equals(obj: Any): Boolean = obj match {
      case Player(username, _) => this.userId == username
      case _                   => false
    }
  }

  enum Msg:
    case Join(player: Player)
    case ReadyToggle(userId: String)
    case Move(player: Player, x: Int, y: Int)

  private def apply(hub: ActorRef[OutgoingMessage], username: String): Behavior[Msg] = Behaviors.setup { context =>
    val player1     = Player(username)
    val intialState = Waiting(player1, None)
    new GameLogic(hub, context).waiting(intialState)
  }

  object Ready {
    val READY = 11
    def unapply(action: Action): Option[String] =
      if (action.tpe == READY) {
        Some(action.userId)
      } else None
  }

  object Move {
    val MOVE = 12
    def unapply(action: Action): Option[(String, Int, Int)] =
      if (action.tpe == MOVE) {
        action.payload.flatMap { payload =>
          for {
            x <- payload("x").flatMap(_.asNumber).flatMap(_.toInt)
            y <- payload("y").flatMap(_.asNumber).flatMap(_.toInt)
          } yield (action.userId, x, y)
        }
      } else None
  }

  def adapter(hub: ActorRef[OutgoingMessage], username: String): Behavior[RoomMessage] = Behaviors.setup(context =>
    val gameLogic = context.spawnAnonymous(GameLogic(hub, username))
    val convert: RoomMessage => Msg = {
      case Action.JoinRoom(userId) =>
        Msg.Join(Player(userId))
      case RoomMessage.UserJoined(userId) =>
        Msg.Join(Player(userId))
      case Ready(userId) =>
        Msg.ReadyToggle(userId)
      case GameLogic.Move(userId, x, y) =>
        Msg.Move(Player(userId), x, y)
    }
    Behaviors.receiveMessage[RoomMessage] { msg =>
      gameLogic ! convert(msg)
      Behaviors.same
    }
  )
