package tictactoe

import tictactoe.logic.Lobby.GameRoom
import io.scrabit.actor.message.OutgoingMessage
import io.circe.Json
import io.circe.syntax.*
import tictactoe.logic.GameLogic.Player
import tictactoe.logic.GameLogic.Board
import tictactoe.logic.GameLogic.Mark
import tictactoe.logic.GameLogic.Cell

package object message {

  object out {
    case class LobbyInfo(recipient: String, rooms: List[GameRoom]) extends OutgoingMessage {
      override val tpe: Int = 12

      override def data: Json = Json.obj(
        "rooms" ->
          Json.arr(rooms.map(room => Json.obj("id" -> room.id.asJson, "owner" -> room.owner.asJson))*)
      )
    }

    case class UserReady(recipient: String, joiner: String) extends OutgoingMessage {

      override def tpe: Int = ???

      override def data: Json = ???
    }

    case class BoardSync(recipient: String, currentTurn: Player, board: Board) extends OutgoingMessage {
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

    case class GameResult(recipient: String, winner: Option[Player]) extends OutgoingMessage {

      override def tpe: Int = 14

      override def data: Json = Json.obj("winner" -> winner.map(_.userId).asJson)

    }
  }

}
