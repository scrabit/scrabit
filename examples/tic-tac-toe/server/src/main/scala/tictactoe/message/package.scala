package tictactoe

import tictactoe.logic.Lobby.GameRoom
import io.scrabit.actor.message.OutgoingMessage
import io.circe.Json
import io.circe.syntax.*
import tictactoe.logic.GameLogic.*

package object message {

  object out {
    private val LOBBY_INFO  = 10
    private val READY_SYNC  = 11
    private val BOARD_SYNC  = 12
    private val GAME_RESULT = 13

    case class LobbyInfo(recipient: String, rooms: List[GameRoom]) extends OutgoingMessage {
      override val tpe: Int = LOBBY_INFO

      override def data: Json = Json.obj(
        "rooms" ->
          Json.arr(rooms.map(room => Json.obj("id" -> room.id.asJson, "owner" -> room.owner.asJson))*)
      )
    }

    case class ReadySync(recipient: String, players: List[Player]) extends OutgoingMessage {

      override def tpe: Int = READY_SYNC

      override def data: Json = Json.obj(
        "ready" -> Json.arr(
          players.map(player =>
            Json.obj(
              "userId" -> player.userId.asJson,
              "ready"  -> player.isReady.asJson
            )
          )*
        )
      )
    }

    case class BoardSync(recipient: String, currentTurn: Player, board: Board) extends OutgoingMessage {
      override val tpe: Int = BOARD_SYNC

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

      override def tpe: Int = GAME_RESULT

      override def data: Json = Json.obj("winner" -> winner.map(_.userId).asJson)

    }
  }

}
