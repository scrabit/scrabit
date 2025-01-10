package io.scrabit.actor.message

import io.circe.Json
import io.circe.syntax.*
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}

trait OutgoingMessage {
  def recipient: String
  def tpe: Int
  def data: Json
  def toWsMessage: Message = TextMessage.Strict(
    Json
      .obj(
        "userId"  -> recipient.asJson, // TODO: REMOVE this field ?
        "tpe"     -> tpe.asJson,
        "payload" -> data
      )
      .noSpaces
  )
}

object OutgoingMessage:

  val LOGIN_SUCCESS        = 0
  private val LOGIN_FAILED = 1
  val USER_JOINED_ROOM     = 2
  val ROOM_CREATED         = 3
  private val USER_READY   = 5
  private val GAME_START   = 6
  private val NEW_ROUND    = 7
  private val ROUND_END    = 9

  case class LoginSuccess(recipient: String, sessionKey: String) extends OutgoingMessage {
    override val tpe: Int = LOGIN_SUCCESS

    override def data: Json = Json.obj("sessionKey" -> sessionKey.asJson)
  }

  case class LoginFailed(recipient: String, error: String) extends OutgoingMessage {
    override val tpe: Int = LOGIN_FAILED

    override def data: Json = Json.obj("error" -> error.asJson)
  }

  case class UserJoinedRoom(recipient: String, roomId: Int) extends OutgoingMessage {
    override val tpe: Int = USER_JOINED_ROOM

    override def data: Json = Json.obj("roomId" -> roomId.asJson)
  }

end OutgoingMessage
