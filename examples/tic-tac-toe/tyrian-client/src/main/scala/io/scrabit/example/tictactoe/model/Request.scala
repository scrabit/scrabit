package io.scrabit.example.tictactoe.model

import io.circe.Json
import io.circe.syntax.*

sealed trait Request:
  def sessionKey: String
  def tpe: Int
  def payload: Option[Json]

  def toJson: String =
    io.circe.Json.obj(
      "sessionKey" -> sessionKey.asJson,
      "tpe"        -> tpe.asJson,
      "payload"    -> payload.asJson
    ).noSpaces

object Request:
  private val CREATE_ROOM = 3
  private val JOIN_ROOM   = 2
  private val READY       = 11
  private val MOVE        = 12

  case class CreateRoom(name: String, sessionKey: String) extends Request:
    val tpe: Int = CREATE_ROOM
    val payload: Option[Json] = Some(Json.obj("roomName" -> name.asJson))

  case class JoinRoom(roomId: Int, sessionKey: String) extends Request:
    val tpe: Int = JOIN_ROOM
    val payload: Option[Json] = Some(Json.obj("roomId" -> roomId.asJson))

  case class Ready(sessionKey: String) extends Request:
    val tpe: Int = READY
    val payload: Option[Json] = None

  case class Move(x: Int, y: Int, sessionKey: String) extends Request:
    val tpe: Int = MOVE
    val payload: Option[Json] = Some(Json.obj("x" -> x.asJson, "y" -> y.asJson))
