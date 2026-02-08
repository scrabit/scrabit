package io.scrabit.example.tictactoe.indigo.network

import io.scrabit.example.tictactoe.indigo.model.*
import io.circe.JsonObject
import io.circe.Json
import io.circe.syntax.*

case class ServerMessage(userId: String, tpe: Int, data: JsonObject)

object Protocol:

  private val LOGIN_SUCCESS    = 0
  private val LOGIN_FAILED     = 1
  private val USER_JOINED_ROOM = 2
  private val ROOM_CREATED     = 3
  private val LOBBY_INFO       = 10
  private val READY_SYNC       = 11
  private val BOARD_SYNC       = 12
  private val GAME_RESULT      = 13

  private val REQUEST_JOIN_ROOM   = 2
  private val REQUEST_CREATE_ROOM = 3
  private val REQUEST_READY       = 11
  private val REQUEST_MOVE        = 12

  def parse(raw: String): Option[ServerMessage] =
    for
      json   <- io.circe.parser.parse(raw).toOption
      obj    <- json.asObject
      userId <- obj("userId").flatMap(_.asString)
      tpe    <- obj("tpe").flatMap(_.asNumber).flatMap(_.toInt)
      data   <- obj("payload").flatMap(_.asObject)
    yield ServerMessage(userId, tpe, data)

  def mkLogin(user: String, pass: String): String =
    s"LOGIN-$user/$pass"

  def mkRequest(sessionKey: String, tpe: Int, payload: Option[Json]): String =
    Json
      .obj(
        "sessionKey" -> sessionKey.asJson,
        "tpe"        -> tpe.asJson,
        "payload"    -> payload.asJson
      )
      .noSpaces

  def mkCreateRoom(sessionKey: String, name: String): String =
    mkRequest(sessionKey, REQUEST_CREATE_ROOM, Some(Json.obj("roomName" -> name.asJson)))

  def mkJoinRoom(sessionKey: String, roomId: Int): String =
    mkRequest(sessionKey, REQUEST_JOIN_ROOM, Some(Json.obj("roomId" -> roomId.asJson)))

  def mkReady(sessionKey: String): String =
    mkRequest(sessionKey, REQUEST_READY, None)

  def mkMove(sessionKey: String, x: Int, y: Int): String =
    mkRequest(sessionKey, REQUEST_MOVE, Some(Json.obj("x" -> x.asJson, "y" -> y.asJson)))

  object LoginSuccess:
    def unapply(msg: ServerMessage): Option[String] =
      if msg.tpe != LOGIN_SUCCESS then None
      else msg.data("sessionKey").flatMap(_.asString)

  object LoginFailed:
    def unapply(msg: ServerMessage): Option[String] =
      if msg.tpe != LOGIN_FAILED then None
      else msg.data("error").flatMap(_.asString)

  object LobbyInfo:
    def unapply(msg: ServerMessage): Option[List[Room]] =
      if msg.tpe != LOBBY_INFO then None
      else
        msg.data("rooms").flatMap(_.asArray).map { rooms =>
          rooms.flatMap { room =>
            for
              roomObj <- room.asObject
              id      <- roomObj("id").flatMap(_.asNumber).flatMap(_.toInt)
              name    <- roomObj("name").flatMap(_.asString)
              owner   <- roomObj("owner").flatMap(_.asString)
            yield Room(id, owner, name)
          }.toList
        }

  object JoinedRoom:
    def unapply(msg: ServerMessage): Option[(Int, String)] =
      if msg.tpe != USER_JOINED_ROOM then None
      else
        for
          roomId <- msg.data("roomId").flatMap(_.asNumber).flatMap(_.toInt)
          userId <- msg.data("userId").flatMap(_.asString)
        yield (roomId, userId)

  object RoomCreated:
    def unapply(msg: ServerMessage): Option[(Int, String)] =
      if msg.tpe != ROOM_CREATED then None
      else
        for
          roomId   <- msg.data("roomId").flatMap(_.asNumber).flatMap(_.toInt)
          roomName <- msg.data("roomName").flatMap(_.asString)
        yield (roomId, roomName)

  object ReadySync:
    def unapply(msg: ServerMessage): Option[List[Player]] =
      if msg.tpe != READY_SYNC then None
      else
        msg.data("ready").flatMap(_.asArray).map { players =>
          players.flatMap { player =>
            for
              playerObj <- player.asObject
              userId    <- playerObj("userId").flatMap(_.asString)
              ready     <- playerObj("ready").flatMap(_.asBoolean)
            yield Player(userId, ready)
          }.toList
        }

  object BoardSync:
    def unapply(msg: ServerMessage): Option[(String, Board)] =
      if msg.tpe != BOARD_SYNC then None
      else
        for
          currentTurn <- msg.data("currentTurn").flatMap(_.asString)
          cellsJson   <- msg.data("cells").flatMap(_.asArray)
          cells = cellsJson.map { row =>
            row.asArray
              .map { rowCells =>
                rowCells.map { cell =>
                  cell.asString match
                    case Some("X") => Cell(Some(Mark.X))
                    case Some("O") => Cell(Some(Mark.O))
                    case _         => Cell(None)
                }.toVector
              }
              .getOrElse(Vector.empty)
          }.toVector
        yield (currentTurn, Board(cells))

  object GameResult:
    def unapply(msg: ServerMessage): Option[Option[String]] =
      if msg.tpe != GAME_RESULT then None
      else Some(msg.data("winner").flatMap(_.asString))
