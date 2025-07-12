package io.scrabit.example.tictactoe.model

import io.circe.Json
import io.circe.syntax.*

sealed trait Request {
  def sessionKey: String
  def tpe: Int  
  def payload: Option[Json]

  def asJsonObject: io.circe.JsonObject = 
    io.circe.JsonObject(
      "sessionKey" -> sessionKey.asJson,
      "tpe" -> tpe.asJson,
      "payload" -> payload.asJson
    )
}

object Request {
  // Request type constants
  private val CREATE_ROOM = 3
  private val JOIN_ROOM   = 2
  private val READY       = 11

  case class CreateRoomRequest(name: String) extends Request {
    override def sessionKey: String = "" // Will be set when sending
    override def tpe: Int = CREATE_ROOM
    override def payload: Option[Json] = Some(Json.obj("roomName" -> name.asJson))
  }

  case class JoinRoomRequest(roomId: Int) extends Request {
    override def sessionKey: String = "" // Will be set when sending  
    override def tpe: Int = JOIN_ROOM
    override def payload: Option[Json] = Some(Json.obj("roomId" -> roomId.asJson))
  }

  case class ReadyRequest() extends Request {
    override def sessionKey: String = "" // Will be set when sending
    override def tpe: Int = READY
    override def payload: Option[Json] = None
  }
} 