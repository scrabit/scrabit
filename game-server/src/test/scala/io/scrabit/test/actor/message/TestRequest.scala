package io.scrabit.test.actor.message

import io.circe.{Json, JsonObject}
import io.circe.syntax.given
import io.scrabit.actor.message.IncomingMessage.{RawMessage, Request}
import io.scrabit.actor.message.RoomMessage.ActionType
import io.scrabit.actor.message.{IncomingMessage, OutgoingMessage}
import org.apache.pekko.actor.typed.ActorRef
import io.scrabit.actor.message.RoomMessage.Action
import io.scrabit.actor.message.RoomMessage.given

object TestRequest {

  def login(username: String, password: String, connection: ActorRef[OutgoingMessage]): RawMessage =
    RawMessage(s"LOGIN-$username/$password", connection)

  def createRoom(userId: String, name: String): Request =
    Request(userId, IncomingMessage.Request.CREATE_ROOM, Json.obj("roomName" -> name.asJson).asObject)

  def action(userId: String, tpe: ActionType, data: Option[JsonObject]): Request =
    Request(userId, tpe, data)

  def sessionMessage(userId: String, tpe: ActionType, sessionKey: String, connection: ActorRef[OutgoingMessage]): RawMessage =
    val textData = Json.obj("userId" -> userId.asJson, "sessionKey" -> sessionKey.asJson, "tpe" -> tpe.asJson).noSpaces
    RawMessage(textData, connection)
}
