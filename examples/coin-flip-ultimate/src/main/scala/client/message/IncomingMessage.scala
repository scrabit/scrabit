package client.message

import io.circe.JsonObject
import io.scrabit.actor.message.OutgoingMessage as ServerMessage

case class IncomingMessage(userId: String, tpe: Int, data: JsonObject)

object IncomingMessage:
  object LoginSuccess {
    def unapply(msg: IncomingMessage): Option[(String, String)] =
      println(s"msg data: ${msg.data}")
      if msg.tpe != ServerMessage.LOGIN_SUCCESS then None
      else msg.data("sessionKey").flatMap(_.asString).map(sessionKey =>
        (msg.userId, sessionKey)
      )
  }
