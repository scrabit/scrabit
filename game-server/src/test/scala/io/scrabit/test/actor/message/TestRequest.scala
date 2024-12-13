package io.scrabit.test.actor.message

import io.circe.{Json, JsonObject}
import io.circe.syntax.given
import io.scrabit.actor.message.IncomingMessage.{RawMessage, Request}
import io.scrabit.actor.message.RoomMessage.ActionType
import io.scrabit.actor.message.{IncomingMessage, OutgoingMessage}
import org.apache.pekko.actor.typed.ActorRef

object TestRequest {
  
  def login(username: String, password: String, connection: ActorRef[OutgoingMessage]): RawMessage = {
    RawMessage(s"LOGIN-$username/$password", connection)  
  }
  
  def createRoom(userId: String, name: String): Request = {
    Request(userId, IncomingMessage.Request.CREATE_ROOM, 
      Json.obj("roomName" -> name.asJson).asObject
    )
  }
  
  def action(userId: String, tpe: ActionType, data: Option[JsonObject]): Request = {
    Request(userId, tpe, data)
  }
}
