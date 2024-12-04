package io.scrabit.actor.message

import org.apache.pekko.http.scaladsl.model.ws.Message
import io.circe.Json, io.circe.syntax._
import io.circe.JsonObject
import org.apache.pekko.http.scaladsl.model.ws.TextMessage

trait OutgoingMessage {
  def userId: String
  def tpe: Int
  def data: Json
  def toWsMessage: Message = TextMessage.Strict(
    Json
      .obj(
        "userId" -> userId.asJson,
        "tpe"    -> tpe.asJson,
        "data"   -> data
      )
      .noSpaces
  )
}

object OutgoingMessage:

  val LOGIN_SUCCESS    = 0
  private val LOGIN_FAILED     = 1
  val USER_JOINED_ROOM = 2
  val ROOM_CREATED = 3
  private val USER_READY       = 5
  private val GAME_START       = 6
  private val NEW_ROUND        = 7
  private val ROUND_END        = 9

  case class LoginSuccess(userId: String, sessionKey: String) extends OutgoingMessage {
    override val tpe: Int = LOGIN_SUCCESS

    override def data: Json = Json.obj("sessionKey" -> sessionKey.asJson)
  }

  case class LoginFailed(userId: String, error: String) extends OutgoingMessage {
    override val tpe: Int = LOGIN_FAILED

    override def data: Json = Json.obj("error" -> error.asJson)
  }

  case class UserJoinedRoom(userId: String, roomId: Int) extends OutgoingMessage {
    override val tpe: Int = USER_JOINED_ROOM

    override def data: Json = Json.obj("roomId" -> roomId.asJson)
  }

end OutgoingMessage
