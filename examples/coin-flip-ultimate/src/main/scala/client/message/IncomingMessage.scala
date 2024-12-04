package client.message

import io.circe.JsonObject
import io.scrabit.actor.message.OutgoingMessage as ServerMessage

case class IncomingMessage(userId: String, tpe: Int, data: JsonObject)

object IncomingMessage:
  private val GAME_RESULT = 12
  
  object LoginSuccess {
    def unapply(msg: IncomingMessage): Option[(String, String)] =
      println(s"msg data: ${msg.data}")
      if msg.tpe != ServerMessage.LOGIN_SUCCESS then None
      else msg.data("sessionKey").flatMap(_.asString).map(sessionKey =>
        (msg.userId, sessionKey)
      )
  }

  object RoomJoined {
    def unapply(msg: IncomingMessage): Option[(String, Int)] =
      if msg.tpe != ServerMessage.USER_JOINED_ROOM then None
      else msg.data("roomId").flatMap(_.asNumber.flatMap(_.toInt)).map(roomId =>
        (msg.userId, roomId)
      )
  }
  
  object GameResult {
    def unapply(msg: IncomingMessage): Option[String] =
      if msg.tpe != GAME_RESULT then None
      else msg.data("message").flatMap(_.asString)
  }
